package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.dto.TemplateContentView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.repository.OrganizationRepository;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Admin-driven CRUD for {@link DocumentTemplate}, scoped to the calling
 * admin's active organization. All write methods require the org id so
 * the caller (bean / servlet) can never operate on another tenant's row.
 */
@ApplicationScoped
public class TemplateManagementService {

    private static final String DEFAULT_MIME_TYPE = "application/pdf";

    private final DocumentTemplateRepository templateRepo;

    private final OrganizationRepository organizationRepo;

    private final Logger logger;

    @Inject
    public TemplateManagementService(DocumentTemplateRepository templateRepo,
                                     OrganizationRepository organizationRepo,
                                     Logger logger) {
        this.templateRepo = templateRepo;
        this.organizationRepo = organizationRepo;
        this.logger = logger;
    }

    @Transactional
    public List<DocumentTemplateView> listByOrganization(@Nullable UUID organizationId) {
        logger.tracef("Listing active templates in organization %s", organizationId);
        if (organizationId == null) {
            return List.of();
        }

        return templateRepo.listActiveByOrganizationId(organizationId).stream()
                .map(DocumentTemplate::toView)
                .toList();
    }

    @Transactional
    public DocumentTemplateView createTemplate(@NotNull UUID organizationId,
                                               @NotBlank String name,
                                               @Nullable String description,
                                               @NotNull DocumentTemplate.DocType docType,
                                               @NotNull byte[] data,
                                               @Nullable String mimeType) {
        logger.tracef("Creating template %s (%s, %d bytes, %s) in organization %s",
                name, docType, data == null ? 0 : data.length, mimeType, organizationId);
        final var organization = organizationRepo.findById(organizationId)
                .orElseThrow(() -> BusinessException.unknownOrganization(organizationId));

        final var template = new DocumentTemplate()
                .setOrganization(organization)
                .setName(name.trim())
                .setDescription(description == null ? null : description.trim())
                .setDocType(docType)
                .setTemplateData(data)
                .setMimeType(mimeType == null || mimeType.isBlank() ? DEFAULT_MIME_TYPE : mimeType)
                .setActive(true);
        templateRepo.insertWithSession(template);
        logger.debugf("Created document template %s (%s) in organization %s",
                template.getId(), docType, organizationId);
        return template.toView();
    }

    /**
     * Hard-delete. Caller passes the active org id so a request for a row
     * belonging to a different tenant is silently no-op'd.
     */
    @Transactional
    public void deleteTemplate(@Nullable UUID organizationId, @Nullable UUID templateId) {
        logger.tracef("Deleting template %s on behalf of organization %s", templateId, organizationId);
        if (organizationId == null || templateId == null) {
            return;
        }

        final var match = templateRepo.findById(templateId);
        if (match.isEmpty()) {
            logger.debugf("Skipped deleting template %s -- not found", templateId);
            return;
        }

        if (!match.get().getOrganization().getId().equals(organizationId)) {
            logger.debugf("Skipped deleting template %s -- caller organization %s does not own it (belongs to %s)",
                    templateId, organizationId, match.get().getOrganization().getId());
            return;
        }

        templateRepo.deleteById(templateId);
        logger.debugf("Deleted document template %s in organization %s", templateId, organizationId);
    }

    /**
     * Loads a template's binary content for the preview servlet. Returns
     * {@code null} if the row doesn't exist or belongs to a different
     * organization than the caller's active one.
     */
    @Transactional
    @Nullable
    public TemplateContentView loadForPreview(@Nullable UUID organizationId, @Nullable UUID templateId) {
        logger.tracef("Loading template %s for preview on behalf of organization %s", templateId, organizationId);
        if (organizationId == null || templateId == null) {
            return null;
        }

        return templateRepo.findById(templateId)
                .filter(template -> template.getOrganization().getId().equals(organizationId))
                .map(template -> new TemplateContentView(template.getTemplateData(), template.getName(), template.getMimeType()))
                .orElse(null);
    }

}
