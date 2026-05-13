package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.repository.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Admin-driven CRUD for {@link DocumentTemplate}, scoped to the calling
 * admin's active organization. All write methods require the org id so
 * the caller (bean / servlet) can never operate on another tenant's row.
 */
@ApplicationScoped
public class TemplateManagementService {

    @Inject
    DocumentTemplateRepository templateRepo;

    @Inject
    OrganizationRepository organizationRepo;

    public List<DocumentTemplateView> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        return templateRepo.listActiveByOrganizationId(organizationId).stream()
                .map(DocumentTemplate::toView)
                .toList();
    }

    @Transactional
    public DocumentTemplateView createTemplate(Long organizationId,
                                               String name,
                                               String description,
                                               DocumentTemplate.DocType docType,
                                               byte[] data,
                                               String mimeType) {
        Organization org = organizationRepo.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + organizationId));

        DocumentTemplate t = new DocumentTemplate();
        t.setOrganization(org);
        t.setName(name.trim());
        t.setDescription(description == null ? null : description.trim());
        t.setDocType(docType);
        t.setTemplateData(data);
        t.setMimeType(mimeType == null || mimeType.isBlank() ? "application/pdf" : mimeType);
        t.setIsActive(Boolean.TRUE);
        templateRepo.save(t);
        return t.toView();
    }

    /**
     * Hard-delete. Caller passes the active org id so a request for a row
     * belonging to a different tenant is silently no-op'd.
     */
    @Transactional
    public void deleteTemplate(Long organizationId, Long templateId) {
        if (organizationId == null || templateId == null) return;
        Optional<DocumentTemplate> match = templateRepo.findById(templateId);
        if (match.isEmpty()) return;
        if (!match.get().getOrganization().getId().equals(organizationId)) return;
        templateRepo.deleteById(templateId);
    }

    /**
     * Loads a template's binary content for the preview servlet. Returns
     * {@code null} if the row doesn't exist or belongs to a different
     * organization than the caller's active one.
     */
    public TemplateContent loadForPreview(Long organizationId, Long templateId) {
        if (organizationId == null || templateId == null) return null;
        return templateRepo.findById(templateId)
                .filter(t -> t.getOrganization().getId().equals(organizationId))
                .map(t -> new TemplateContent(t.getTemplateData(), t.getName(), t.getMimeType()))
                .orElse(null);
    }

    public record TemplateContent(byte[] data, String fileName, String mimeType) {
    }
}
