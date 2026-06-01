package com.oppshan.securedoc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jboss.logging.Logger;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code {{tokenName}}} markers in a flat PDF template with
 * caller-supplied values, in place. Called by
 * {@link DocumentGenerationService#generate} at issuance time -- one
 * substitution pass per request, no DB persistence of marker geometry
 * (the upload-time {@code template_field} optimisation in
 * pdf-only-templates plan stays deferred).
 *
 * <h3>How it works</h3>
 * A {@link PDFTextStripper} subclass walks every glyph in the document,
 * groups them by page + baseline (with a small epsilon for kerning jitter),
 * sorts each line left-to-right, and stitches the per-glyph characters back
 * into a single string per line -- which recovers {@code {{tokenName}}}
 * even when Word split it across separate text runs. The pattern
 * {@code \{\{([a-zA-Z][a-zA-Z0-9]*)\}\}} is then matched against each
 * stitched line; every hit yields a bounding box (first matched glyph's
 * baselineX through last matched glyph's right edge, at the captured font
 * size). For each match with a provided value, the renderer covers the
 * marker with a white rectangle and draws the substitution at the same
 * baseline.
 *
 * <h3>Oversized values</h3>
 * If the substitution doesn't fit at the marker's font size, the renderer
 * steps the size down by 0.5pt at a time until it fits or hits the
 * {@value #MIN_FONT_SIZE}pt floor (per the "TO finalize" section of the
 * parent plan -- font-auto-shrink is the only strategy that stays in
 * bounds without data loss and keeps output bytes deterministic for the
 * future signing chain).
 *
 * <h3>Unknown tokens / no value</h3>
 * Tokens missing from the value map are logged at WARN and the marker
 * stays visible in the output -- intentional, so authoring mistakes
 * (typos, unsupported tokens) surface immediately instead of silently
 * producing blank fields.
 *
 * <h3>Known limitation: text extraction sees both layers</h3>
 * The white-rectangle cover only hides the marker <em>visually</em> -- the
 * original {@code Tj}/{@code TJ} text-show operations remain in the page's
 * content stream. PDF viewers, printers, and screenshots show the
 * substitution cleanly (the rect occludes the marker), but copy-paste and
 * {@link PDFTextStripper} extract both layers. Acceptable for the issued
 * certificate use case (recipients view or print, rarely copy-paste);
 * fully removing the underlying op would require content-stream editing
 * and is deferred until a real workflow forces it.
 */
@ApplicationScoped
public class TemplateMarkerRenderer {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9]*)\\}\\}");

    // Two glyphs are on the same line if their baseline Ys differ by less
    // than this many points. ~1.5pt covers Word's kerning jitter without
    // merging adjacent paragraphs.
    private static final float BASELINE_EPSILON = 1.5f;

    private static final float MIN_FONT_SIZE = 8f;

    // Inflate the white cover rectangle slightly so the original glyph's
    // antialiasing edges don't peek out around the substitution.
    private static final float COVER_PADDING = 1f;

    private final Logger logger;

    @Inject
    public TemplateMarkerRenderer(Logger logger) {
        this.logger = logger;
    }

    public Result renderFilled(byte[] templateBytes, Map<String, String> values) {
        try (final var document = Loader.loadPDF(templateBytes)) {
            final var collector = new GlyphCollector();
            collector.setSortByPosition(true);
            collector.getText(document);

            final var markers = findMarkers(collector.getGlyphs());
            if (markers.isEmpty()) {
                // Preserve byte-identity for templates with no markers.
                return new Result(templateBytes, 0, 0);
            }

            int substituted = 0;
            for (final var marker : markers) {
                final var value = values.get(marker.tokenName());
                if (value == null) {
                    logger.warnf("No value for token '%s' on page %d -- marker left visible",
                            marker.tokenName(), marker.pageIndex() + 1);
                    continue;
                }
                drawSubstitution(document, marker, value);
                substituted++;
            }

            if (substituted == 0) {
                // Nothing actually changed -- skip the reserialize round-trip.
                return new Result(templateBytes, markers.size(), 0);
            }

            final var output = new ByteArrayOutputStream();
            document.save(output);
            logger.debugf("Marker render complete: %d found, %d substituted", markers.size(), substituted);
            return new Result(output.toByteArray(), markers.size(), substituted);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to render marker substitutions on template", failure);
        }
    }

    private static List<Marker> findMarkers(List<Glyph> glyphs) {
        // Group by page; within a page, group by baseline (epsilon match).
        final Map<Integer, List<List<Glyph>>> linesByPage = new HashMap<>();
        for (final var glyph : glyphs) {
            final var pageLines = linesByPage.computeIfAbsent(glyph.pageIndex(), key -> new ArrayList<>());
            List<Glyph> targetLine = null;
            for (final var line : pageLines) {
                if (!line.isEmpty()
                        && Math.abs(line.get(0).baselineY() - glyph.baselineY()) < BASELINE_EPSILON) {
                    targetLine = line;
                    break;
                }
            }
            if (targetLine == null) {
                targetLine = new ArrayList<>();
                pageLines.add(targetLine);
            }
            targetLine.add(glyph);
        }

        final List<Marker> markers = new ArrayList<>();
        for (final var entry : linesByPage.entrySet()) {
            final int pageIndex = entry.getKey();
            for (final var line : entry.getValue()) {
                line.sort(Comparator.comparing(Glyph::baselineX));
                final var stitched = new StringBuilder(line.size());
                for (final var glyph : line) {
                    stitched.append(glyph.character());
                }

                final var matcher = TOKEN_PATTERN.matcher(stitched);
                while (matcher.find()) {
                    final int startIdx = matcher.start();
                    final int endIdx = matcher.end() - 1;
                    final var firstGlyph = line.get(startIdx);
                    final var lastGlyph = line.get(endIdx);
                    final var boxX = firstGlyph.baselineX();
                    final var baselineY = firstGlyph.baselineY();
                    final var boxWidth = (lastGlyph.baselineX() + lastGlyph.width()) - firstGlyph.baselineX();
                    final var fontSize = firstGlyph.fontSize();
                    markers.add(new Marker(matcher.group(1), pageIndex, boxX, baselineY, boxWidth, fontSize));
                }
            }
        }
        return markers;
    }

    private static void drawSubstitution(PDDocument document, Marker marker, String value)
            throws IOException {
        final var page = document.getPage(marker.pageIndex());
        final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final var safeValue = sanitize(value);
        final var fontSize = fitFontSize(font, safeValue, marker.width(), marker.fontSize());

        try (final var content = new PDPageContentStream(
                document, page, AppendMode.APPEND, true, true)) {
            // Cover the marker text with a white rectangle.
            content.setNonStrokingColor(Color.WHITE);
            content.addRect(
                    marker.x() - COVER_PADDING,
                    marker.baselineY() - COVER_PADDING,
                    marker.width() + 2f * COVER_PADDING,
                    marker.fontSize() + COVER_PADDING
            );
            content.fill();

            // Draw the value at the captured baseline.
            content.setNonStrokingColor(Color.BLACK);
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(marker.x(), marker.baselineY());
            content.showText(safeValue);
            content.endText();
        }
    }

    private static float fitFontSize(PDFont font, String value, float maxWidth, float startSize)
            throws IOException {
        if (maxWidth <= 0f || value.isEmpty()) {
            return Math.max(MIN_FONT_SIZE, startSize);
        }
        float fontSize = startSize;
        while (fontSize > MIN_FONT_SIZE) {
            final var widthAtSize = font.getStringWidth(value) / 1000f * fontSize;
            if (widthAtSize <= maxWidth) {
                return fontSize;
            }
            fontSize -= 0.5f;
        }
        return MIN_FONT_SIZE;
    }

    /**
     * Helvetica is WinAnsi-encoded -- drop anything {@code showText} can't
     * render so a stray unicode codepoint in a value can't blow up the
     * substitution. Mirrors the helper of the same name in
     * {@link DocumentGenerationService}.
     */
    private static String sanitize(String value) {
        final var builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character == '\n' || character == '\r' || character == '\t') {
                builder.append(' ');
            } else if (character < 0x20 || (character >= 0x7F && character <= 0x9F) || character > 0xFF) {
                builder.append('?');
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static final class GlyphCollector extends PDFTextStripper {

        private final List<Glyph> glyphs = new ArrayList<>();

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            for (final var position : positions) {
                final var unicode = position.getUnicode();
                if (unicode == null || unicode.isEmpty()) {
                    continue;
                }
                // The text matrix's translateY is the bottom-up user-space baseline
                // PDPageContentStream.newLineAtOffset wants -- exactly the value Td
                // wrote into the content stream. Going through
                // pageHeight - getY() - getHeight() instead is off by a few points
                // because TextPosition.getHeight() is the font's bounding-box
                // height, not the ascent, so it overshoots the baseline.
                final var matrix = position.getTextMatrix();
                glyphs.add(new Glyph(
                        getCurrentPageNo() - 1,
                        unicode.charAt(0),
                        matrix.getTranslateX(),
                        matrix.getTranslateY(),
                        position.getWidth(),
                        position.getFontSizeInPt()
                ));
            }
        }

        List<Glyph> getGlyphs() {
            return glyphs;
        }
    }

    private record Glyph(int pageIndex,
                         char character,
                         float baselineX,
                         float baselineY,
                         float width,
                         float fontSize) {
    }

    private record Marker(String tokenName,
                          int pageIndex,
                          float x,
                          float baselineY,
                          float width,
                          float fontSize) {
    }

    public record Result(byte[] bytes,
                         int markersFound,
                         int markersSubstituted) {
    }
}
