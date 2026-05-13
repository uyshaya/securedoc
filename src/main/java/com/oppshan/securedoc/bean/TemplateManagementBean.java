package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.service.TemplateManagementService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.file.UploadedFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Backs /admin/templates.xhtml. View-scoped — short-lived form state
 * (the upload + table) doesn't need to outlive the page. All ops are
 * scoped to the logged-in admin's active organization, sourced from
 * {@link OrganizationBean}.
 */
@Named
@ViewScoped
public class TemplateManagementBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 9183920473829183741L;

    @Inject
    TemplateManagementService service;

    @Inject
    OrganizationBean organizationBean;

    private List<DocumentTemplateView> templates;

    // ── upload form ──────────────────────────────────────────────
    private String newName;
    private String newDescription;
    private DocumentTemplate.DocType newDocType;
    private UploadedFile newFile;

    @PostConstruct
    void init() {
        reload();
    }

    private void reload() {
        templates = service.listByOrganization(organizationBean.getActiveId());
    }

    public void upload() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Long orgId = organizationBean.getActiveId();
        if (orgId == null) {
            fc.addMessage(null, error("No active organization in session. Please sign in again."));
            return;
        }
        if (newName == null || newName.isBlank()) {
            fc.addMessage(null, error("Template name is required."));
            return;
        }
        if (newDocType == null) {
            fc.addMessage(null, error("Please choose the certificate type for this template."));
            return;
        }
        if (newFile == null || newFile.getSize() <= 0) {
            fc.addMessage(null, error("Please attach a PDF file."));
            return;
        }
        byte[] data;
        try {
            data = newFile.getContent();
        } catch (Exception e) {
            fc.addMessage(null, error("Could not read the uploaded file: " + e.getMessage()));
            return;
        }
        String mime = newFile.getContentType();
        if (mime != null && !mime.equals("application/pdf")) {
            fc.addMessage(null, error("Only PDF files are accepted."));
            return;
        }
        try {
            service.createTemplate(orgId, newName, newDescription, newDocType, data, mime);
        } catch (RuntimeException e) {
            fc.addMessage(null, error("Could not save template: " + e.getMessage()));
            return;
        }
        fc.addMessage(null, info("Template \"" + newName.trim() + "\" uploaded."));
        clearForm();
        reload();
    }

    public void deleteTemplate(DocumentTemplateView templateView) {
        try {
            service.deleteTemplate(organizationBean.getActiveId(), templateView.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    info("Template \"" + templateView.getName() + "\" deleted."));
            reload();
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    error("Could not delete \"" + templateView.getName() + "\": " + e.getMessage()));
        }
    }

    private void clearForm() {
        newName = null;
        newDescription = null;
        newDocType = null;
        newFile = null;
    }

    /** Used by the dataTable to render a friendly label. */
    public String labelOf(DocumentTemplate.DocType type) {
        if (type == null) return "—";
        return switch (type) {
            case BARANGAY_CLEARANCE -> "Barangay Clearance";
            case CERTIFICATE_OF_RESIDENCY -> "Certificate of Residency";
            case CERTIFICATE_OF_INDIGENCY -> "Certificate of Indigency";
        };
    }

    public List<SelectItem> getDocTypeOptions() {
        return Arrays.stream(DocumentTemplate.DocType.values())
                .map(t -> new SelectItem(t, labelOf(t)))
                .toList();
    }

    private static FacesMessage error(String s) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, s, null);
    }

    private static FacesMessage info(String s) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, s, null);
    }

    public List<DocumentTemplateView> getTemplates() {
        return templates;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
    }

    public DocumentTemplate.DocType getNewDocType() {
        return newDocType;
    }

    public void setNewDocType(DocumentTemplate.DocType newDocType) {
        this.newDocType = newDocType;
    }

    public UploadedFile getNewFile() {
        return newFile;
    }

    public void setNewFile(UploadedFile newFile) {
        this.newFile = newFile;
    }
}
