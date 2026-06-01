package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.RequestDetailView;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates the PDF bytes for an issued document. When the request has a
 * template attached, loads its bytes and asks {@link TemplateMarkerRenderer}
 * to fill any {@code {{tokenName}}} markers with values projected from
 * {@link RequestDetailView}; the result is the "official document PDF"
 * persisted by {@link DocumentService} and streamed by
 * {@code RequestDocumentServlet}. When the template is missing (deleted, or
 * a request created before templates were required) falls back to the
 * historical blank-page DRAFT so approve never 500s. Templates with zero
 * markers pass through unchanged.
 *
 * <p>Future work: QR stamping, SHA-256 + ECDSA signing, address-on-Requester so the
 * {@code residentAddress} token has a value source.
 */
@ApplicationScoped
public class DocumentGenerationService {

    private static final DateTimeFormatter GENERATED_AT_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    // Marker-substitution values get a localised, human-readable date. The
    // resident-facing audience is Philippine barangay offices, so the
    // active zone is Asia/Manila regardless of where the JVM is hosted.
    private static final DateTimeFormatter ISSUED_AT_FORMATTER = DateTimeFormatter
            .ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.of("Asia/Manila"));

    private static final float MARGIN = 56f;
    private static final float TITLE_SIZE = 20f;
    private static final float BODY_SIZE = 12f;
    private static final float LINE_GAP = 20f;
    private static final float SECTION_GAP = 14f;

    private final DocumentTemplateRepository templateRepository;

    private final TemplateMarkerRenderer markerRenderer;

    private final Logger logger;

    @Inject
    public DocumentGenerationService(DocumentTemplateRepository templateRepository,
                                     TemplateMarkerRenderer markerRenderer,
                                     Logger logger) {
        this.templateRepository = templateRepository;
        this.markerRenderer = markerRenderer;
        this.logger = logger;
    }

    @Transactional
    public byte[] generate(RequestDetailView request) {
        final var templateId = request.getTemplateId();
        if (templateId != null) {
            final var template = templateRepository.findById(templateId).orElse(null);
            if (template != null) {
                final var templateBytes = template.getTemplateData();
                if (templateBytes != null && templateBytes.length > 0) {
                    final var result = markerRenderer.renderFilled(templateBytes, buildValueMap(request));
                    logger.debugf("Rendered document for request %s (%d markers, %d substituted)",
                            request.getReferenceNumber(),
                            result.markersFound(),
                            result.markersSubstituted());
                    return result.bytes();
                }
            }
            logger.warnf("Template %s for request %s is unavailable -- falling back to DRAFT",
                    templateId, request.getReferenceNumber());
        }
        return generateFallbackDraft(request);
    }

    static Map<String, String> buildValueMap(RequestDetailView detail) {
        final Map<String, String> values = new HashMap<>();
        putIfNotBlank(values, TemplatePlaceholder.RESIDENT_NAME.token(), detail.getFullName());
        putIfNotBlank(values, TemplatePlaceholder.REFERENCE_NUMBER.token(), detail.getReferenceNumber());
        putIfNotBlank(values, TemplatePlaceholder.DATE_ISSUED.token(), ISSUED_AT_FORMATTER.format(Instant.now()));
        putIfNotBlank(values, TemplatePlaceholder.REQUEST_REASON.token(), purposeOf(detail));
        putIfNotBlank(values, TemplatePlaceholder.DOCUMENT_NAME.token(), detail.getDocumentName());
        // RESIDENT_ADDRESS + ORGANIZATION_NAME deferred -- see TemplatePlaceholder javadoc.
        return values;
    }

    private static void putIfNotBlank(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }

    private byte[] generateFallbackDraft(RequestDetailView request) {
        logger.tracef("Generating draft document PDF for request %s", request.getReferenceNumber());
        try (final var document = new PDDocument()) {
            final var page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            final var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            final var regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (final var content = new PDPageContentStream(document, page)) {
                float cursorY = page.getMediaBox().getHeight() - MARGIN;

                cursorY = writeText(content, bold, TITLE_SIZE, cursorY, request.getDocumentName());
                cursorY -= SECTION_GAP;
                cursorY = writeText(content, bold, BODY_SIZE, cursorY, "DRAFT - not yet signed or verified");
                cursorY -= SECTION_GAP;

                cursorY = writeField(content, bold, regular, cursorY, "Reference Number", request.getReferenceNumber());
                cursorY = writeField(content, bold, regular, cursorY, "Status",
                        request.getStatus() == null ? "" : request.getStatus().getLabel());
                cursorY = writeField(content, bold, regular, cursorY, "Generated",
                        GENERATED_AT_FORMATTER.format(Instant.now()));
                cursorY -= SECTION_GAP;

                cursorY = writeText(content, bold, BODY_SIZE, cursorY, "Resident");
                cursorY = writeField(content, bold, regular, cursorY, "Full Name", request.getFullName());
                cursorY = writeField(content, bold, regular, cursorY, "Sex", request.getSexLabel());
                cursorY = writeField(content, bold, regular, cursorY, "Date of Birth", request.getDateOfBirthDisplay());
                cursorY = writeField(content, bold, regular, cursorY, "Email", request.getEmail());
                cursorY = writeField(content, bold, regular, cursorY, "Contact Number", request.getContactNumber());
                cursorY -= SECTION_GAP;

                cursorY = writeText(content, bold, BODY_SIZE, cursorY, "Document");
                cursorY = writeField(content, bold, regular, cursorY, "Type", request.getDocumentName());
                writeField(content, bold, regular, cursorY, "Purpose", purposeOf(request));
            }

            final var output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to generate draft document for request " + request.getReferenceNumber(), failure);
        }
    }

    private static String purposeOf(RequestDetailView request) {
        final var purpose = request.getPurpose();
        final var otherPurpose = request.getOtherPurpose();
        if (otherPurpose != null && !otherPurpose.isBlank()) {
            if (purpose == null || purpose.isBlank()) {
                return otherPurpose;
            }
            return purpose + " (" + otherPurpose + ")";
        }
        return purpose;
    }

    private static float writeText(PDPageContentStream content, PDFont font, float size, float cursorY, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(MARGIN, cursorY);
        content.showText(sanitize(text));
        content.endText();
        return cursorY - (size + 6f);
    }

    private static float writeField(PDPageContentStream content, PDFont labelFont, PDFont valueFont,
                                    float cursorY, String label, String value) throws IOException {
        content.beginText();
        content.newLineAtOffset(MARGIN, cursorY);
        content.setFont(labelFont, BODY_SIZE);
        content.showText(sanitize(label) + ":  ");
        content.setFont(valueFont, BODY_SIZE);
        content.showText(value == null || value.isBlank() ? "N/A" : sanitize(value));
        content.endText();
        return cursorY - LINE_GAP;
    }

    /**
     * Drops characters the Standard-14 Helvetica WinAnsi encoding can't render
     * (control bytes, the C1 range, anything above Latin-1) so {@code showText}
     * never throws on stray input. Good enough for a draft -- the real
     * generator will embed a Unicode font.
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
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
}