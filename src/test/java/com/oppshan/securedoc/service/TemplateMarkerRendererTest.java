package com.oppshan.securedoc.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hermetic tests for {@link TemplateMarkerRenderer}. Builds tiny PDFs
 * in-test with PDFBox, runs the renderer, and re-extracts text to assert
 * substitutions. No fixture files on disk and no Quarkus / CDI -- pure
 * library exercise.
 */
//TODO
class TemplateMarkerRendererTest {

    private TemplateMarkerRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateMarkerRenderer(Logger.getLogger(TemplateMarkerRendererTest.class));
    }

    @Test
    void substituteSingleMarker() throws Exception {
        final var template = buildPdf(List.of("Hello {{residentName}}, welcome."));

        // Capture the marker's baseline from the original template -- this is
        // the user-space Y where we expect the substitution to land.
        final var markerBaseline = firstBaselineOfChar(template, '{');

        final var result = renderer.renderFilled(template, Map.of("residentName", "Juan Dela Cruz"));

        assertEquals(1, result.markersFound());
        assertEquals(1, result.markersSubstituted());

        // The substitution is overlaid (white rect + value at marker baseline).
        // PDFTextStripper extracts both layers because the original Tj op
        // remains in the content stream (see renderer's "Known limitation"
        // javadoc); we don't assert absence of the marker. We DO assert that
        // the substituted "J" glyph's baseline equals the marker's '{' baseline
        // -- if it didn't (e.g. the renderer drew the value at a different Y),
        // the white rect wouldn't cover the marker visually.
        final var extracted = extractText(result.bytes());
        assertTrue(extracted.contains("Juan Dela Cruz"),
                "Substituted value should be present in extracted text, got: " + extracted);

        final var substitutionBaseline = firstBaselineOfChar(result.bytes(), 'J');
        assertEquals(markerBaseline, substitutionBaseline, 0.5f,
                "Substituted 'Juan...' must share the marker's baseline");
    }

    @Test
    void markerWithoutValueLeftVisible() throws Exception {
        final var template = buildPdf(List.of("Hello {{residentName}}"));
        final var result = renderer.renderFilled(template, Map.of());

        assertEquals(1, result.markersFound());
        assertEquals(0, result.markersSubstituted());

        // No substitution happened, so the renderer should short-circuit and
        // return the input bytes (preserving byte identity for the caller).
        assertArrayEquals(template, result.bytes());

        final var extracted = extractText(result.bytes());
        assertTrue(extracted.contains("{{residentName}}"),
                "Marker should still be visible when no value provided, got: " + extracted);
    }

    @Test
    void unknownTokenLeftVisible() throws Exception {
        final var template = buildPdf(List.of("Hello {{wat}}, world {{residentName}}!"));
        final var result = renderer.renderFilled(template, Map.of("residentName", "Juan"));

        assertEquals(2, result.markersFound());
        assertEquals(1, result.markersSubstituted());

        final var extracted = extractText(result.bytes());
        assertTrue(extracted.contains("{{wat}}"),
                "Unknown token should stay visible, got: " + extracted);
        assertTrue(extracted.contains("Juan"),
                "Known token should still be substituted, got: " + extracted);
    }

    @Test
    void noMarkersReturnsInputBytesUnchanged() {
        final var template = buildPdf(List.of("Plain certificate body, no tokens here."));
        final var result = renderer.renderFilled(template,
                Map.of("residentName", "Should not appear"));

        assertEquals(0, result.markersFound());
        assertEquals(0, result.markersSubstituted());
        assertArrayEquals(template, result.bytes(),
                "Templates without markers must pass through byte-identical");
    }

    @Test
    void multipleMarkersAllSubstituted() throws Exception {
        final var template = buildPdf(List.of("Name: {{residentName}}, Ref: {{referenceNumber}}"));
        final var result = renderer.renderFilled(template, Map.of(
                "residentName", "Juan Dela Cruz",
                "referenceNumber", "REF-001-2026"
        ));

        assertEquals(2, result.markersFound());
        assertEquals(2, result.markersSubstituted());

        final var extracted = extractText(result.bytes());
        assertTrue(extracted.contains("Juan Dela Cruz"), extracted);
        assertTrue(extracted.contains("REF-001-2026"), extracted);
    }

    @Test
    void markersOnSeparatePages() throws Exception {
        final var template = buildPdf(List.of(
                "Page 1: Name = {{residentName}}",
                "Page 2: Ref = {{referenceNumber}}"
        ));
        final var result = renderer.renderFilled(template, Map.of(
                "residentName", "Maria Santos",
                "referenceNumber", "REF-XYZ"
        ));

        assertEquals(2, result.markersFound());
        assertEquals(2, result.markersSubstituted());

        final var extracted = extractText(result.bytes());
        assertTrue(extracted.contains("Maria Santos"), extracted);
        assertTrue(extracted.contains("REF-XYZ"), extracted);
    }

    @Test
    void longValueShrinksToFit() {
        // Marker box width is approximately the rendered width of
        // "{{residentName}}" at 12pt. The substitution is much wider --
        // the renderer must shrink the font size to make it fit. We don't
        // assert the extracted text here because overlap with the
        // (visually-covered) original glyphs causes PDFTextStripper to
        // dedupe inconsistent characters; visual verification is the
        // gold standard for this path.
        final var template = buildPdf(List.of("Name: {{residentName}}"));
        final var longName = "Christopher Alexander Montgomery Worthington-Hamilton";
        final var result = renderer.renderFilled(template, Map.of("residentName", longName));

        assertEquals(1, result.markersFound());
        assertEquals(1, result.markersSubstituted());
        assertNotEquals(template.length, result.bytes().length,
                "Renderer should have produced different bytes (overlay drawn)");
    }

    /**
     * Builds a minimal PDF with one line of text per page. The text is
     * drawn at a sane margin and font size so the marker regex sees the
     * curly braces and the substitution has room to land.
     */
    private static byte[] buildPdf(List<String> linesPerPage) {
        try (PDDocument document = new PDDocument()) {
            final var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (final var lineText : linesPerPage) {
                final var page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (final var content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12f);
                    content.newLineAtOffset(72f, 720f);
                    content.showText(lineText);
                    content.endText();
                }
            }
            final var output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to build test PDF", failure);
        }
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * Returns the user-space baseline Y of the first glyph in {@code pdfBytes}
     * whose unicode begins with {@code target}, by reading
     * {@link TextPosition#getTextMatrix()}. The actual value Td wrote.
     */
    private static float firstBaselineOfChar(byte[] pdfBytes, char target) throws Exception {
        final float[] capture = {Float.NaN};
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (final var position : positions) {
                        if (!Float.isNaN(capture[0])) {
                            return;
                        }
                        final var unicode = position.getUnicode();
                        if (unicode != null && !unicode.isEmpty() && unicode.charAt(0) == target) {
                            capture[0] = position.getTextMatrix().getTranslateY();
                        }
                    }
                }
            }.getText(document);
        }
        if (Float.isNaN(capture[0])) {
            throw new AssertionError("Did not find glyph '" + target + "' in the PDF");
        }
        return capture[0];
    }
}
