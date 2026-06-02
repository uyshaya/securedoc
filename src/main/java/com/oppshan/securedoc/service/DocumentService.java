package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.DocumentDownloadView;
import com.oppshan.securedoc.dto.RequestDetailView;
import com.oppshan.securedoc.model.Document;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.repository.DocumentRepository;
import com.oppshan.securedoc.repository.RequestRepository;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the issued-document lifecycle: turning a staff approval into a
 * persisted {@link Document} row tied 1:1 to a {@link Request}, and serving
 * the saved bytes back to {@code RequestDocumentServlet}. Sits alongside
 * {@link RequestService} (which owns request state) -- the approval flow
 * straddles both, and {@link #issueForRequest} runs the whole transition in
 * one transaction so a half-issued request can never appear in the table.
 *
 * <p>PKI signing remains a separate ticket
 * Today the persisted bytes are the
 * {@link com.oppshan.securedoc.model.DocumentTemplate}'s
 * {@code templateData} verbatim (pass-through via
 * {@link DocumentGenerationService}); {@link Document#getOrgCertificateId()}
 * and {@link Document#getDigitalSignature()} are nullable placeholders that
 * will fill in once signing lands.
 */
@ApplicationScoped
public class DocumentService {

    private final RequestRepository requestRepo;

    private final StaffRepository staffRepo;

    private final DocumentRepository documentRepo;

    private final DocumentGenerationService generationService;

    private final RequestService requestService;

    private final Logger logger;

    @Inject
    public DocumentService(RequestRepository requestRepo,
                           StaffRepository staffRepo,
                           DocumentRepository documentRepo,
                           DocumentGenerationService generationService,
                           RequestService requestService,
                           Logger logger) {
        this.requestRepo = requestRepo;
        this.staffRepo = staffRepo;
        this.documentRepo = documentRepo;
        this.generationService = generationService;
        this.requestService = requestService;
        this.logger = logger;
    }

    /**
     * Approves {@code requestId} on behalf of {@code issuedById}: generates
     * the PDF, persists a {@link Document} row, and flips the request to
     * {@link Request.Status#COMPLETED} with {@code processedBy} stamped. The
     * three writes commit together so the request table never shows a
     * COMPLETED row without a matching document (or vice versa).
     *
     * <p>Idempotent: if a Document for the request already exists, returns
     * {@code true} without re-issuing. Tenant-scoped via
     * {@link RequestService#getDetail(UUID, UUID)} -- a wrong-org caller
     * gets {@code false} even if the request id is valid elsewhere.
     */
    @Transactional
    public boolean issueForRequest(UUID requestId, UUID organizationId, UUID issuedById) {
        if (requestId == null || organizationId == null || issuedById == null) {
            return false;
        }
        if (documentRepo.countByRequestId(requestId) > 0L) {
            logger.debugf("Document for request %s already issued -- skipping", requestId);
            return true;
        }

        final var detail = requestService.getDetail(requestId, organizationId).orElse(null);
        if (detail == null) {
            logger.debugf("Issuance rejected -- request %s not visible to organization %s",
                    requestId, organizationId);
            return false;
        }
        final var request = requestRepo.findById(requestId).orElse(null);
        if (request == null) {
            return false;
        }
        final var staff = staffRepo.findById(issuedById).orElse(null);
        if (staff == null) {
            logger.warnf("Issuance rejected -- issuing staff %s not found", issuedById);
            return false;
        }

        final var bytes = generationService.generate(detail);
        final var document = new Document()
                .setRequest(request)
                .setIssuedBy(staff)
                .setFileName(buildFileName(detail))
                .setDocumentData(bytes)
                .setFileSize(bytes.length)
                .setFileHash(sha256Hex(bytes))
                .setVerificationToken(UUID.randomUUID().toString())
                .setIssuedAt(Instant.now());
        documentRepo.insertWithSession(document);

        request.setStatus(Request.Status.COMPLETED).setProcessedBy(staff);
        requestRepo.updateWithSession(request);

        logger.infof("Issued document %s (request %s, file_hash=%s, %d bytes)",
                document.getId(), requestId, document.getFileHash(), bytes.length);
        return true;
    }

    /**
     * Read-only fetch for the document servlet. Returns the bytes + filename
     * for the issued document attached to {@code requestId}, scoped to
     * {@code organizationId} so cross-tenant guessing returns empty.
     */
    @Transactional
    public Optional<DocumentDownloadView> findDownloadForRequest(UUID requestId, UUID organizationId) {
        if (requestId == null || organizationId == null) {
            return Optional.empty();
        }
        return documentRepo.findByRequestIdAndOrganizationId(requestId, organizationId)
                .map(Document::toDownloadView);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException unavailable) {
            // SHA-256 is required by the JCE spec -- if it's missing the JVM is broken.
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private static String buildFileName(RequestDetailView detail) {
        final var rawName = detail.getDocumentName() == null ? "Document" : detail.getDocumentName();
        final var safeName = rawName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        final var fullName = safeName + " - " + detail.getReferenceNumber() + ".pdf";
        return fullName.length() > 255 ? fullName.substring(0, 251) + ".pdf" : fullName;
    }
}
