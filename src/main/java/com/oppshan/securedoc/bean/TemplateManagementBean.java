package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.service.TemplateManagementService;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;
import org.jboss.logging.Logger;
import org.primefaces.model.file.UploadedFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Backs /admin/templates.xhtml. View-scoped: short-lived form state
 * (the upload + table) doesn't need to outlive the page. All ops are
 * scoped to the logged-in admin's active organization, sourced from
 * {@link OrganizationBean}.
 */
@Named
@ViewScoped
public class TemplateManagementBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 9183920473829183741L;

    private final TemplateManagementService templateManagementService;

    private final OrganizationBean organizationBean;

    private final I18n i18n;

    private final Logger logger;

    private List<DocumentTemplateView> templates = List.of();

    // -- upload form ---------------------------------------------
    @Nullable
    private String newName;

    @Nullable
    private String newDescription;

    @Nullable
    private DocumentTemplate.DocType newDocType;

    @Nullable
    private UploadedFile newFile;

    @Inject
    public TemplateManagementBean(TemplateManagementService templateManagementService,
                                  OrganizationBean organizationBean,
                                  I18n i18n,
                                  Logger logger) {
        this.templateManagementService = templateManagementService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected TemplateManagementBean() {
        this(null, null, null, null);
    }

    public void upload() {
        logger.tracef("Uploading template %s (%s) to organization %s",
                newName, newDocType, organizationBean.getActiveId());
        final var facesContext = FacesContext.getCurrentInstance();
        final var organizationId = organizationBean.getActiveId();

        if (organizationId == null) {
            logger.debugf("Rejected template upload -- no active organization in session");
            facesContext.addMessage(null, error(i18n.get("template.upload.no.active.organization")));
            return;
        }

        if (newName == null || newName.isBlank()) {
            logger.debugf("Rejected template upload -- template name is missing");
            facesContext.addMessage(null, error(i18n.get("template.upload.name.required")));
            return;
        }

        if (newDocType == null) {
            logger.debugf("Rejected template upload -- document type is missing");
            facesContext.addMessage(null, error(i18n.get("template.upload.doctype.required")));
            return;
        }

        if (newFile == null || newFile.getSize() <= 0) {
            logger.debugf("Rejected template upload -- no file was attached");
            facesContext.addMessage(null, error(i18n.get("template.upload.file.required")));
            return;
        }

        final byte[] data;
        try {
            data = newFile.getContent();
        } catch (Exception readFailure) {
            logger.warnf(readFailure, "Failed to read uploaded template bytes for organization %s", organizationId);
            facesContext.addMessage(null,
                    error(i18n.get("template.upload.read.failed", readFailure.getMessage())));
            return;
        }

        final var mimeType = newFile.getContentType();
        if (mimeType != null && !mimeType.equals("application/pdf")) {
            logger.debugf("Rejected template upload -- only PDFs are accepted (got %s)", mimeType);
            facesContext.addMessage(null, error(i18n.get("template.upload.pdf.only")));
            return;
        }

        try {
            templateManagementService.createTemplate(organizationId, newName, newDescription, newDocType, data, mimeType);
        } catch (ConstraintViolationException violation) {
            logger.debugf("Template upload in organization %s rejected -- %s constraint violation(s)",
                    organizationId, violation.getConstraintViolations().size());
            violation.getConstraintViolations().forEach(constraintViolation ->
                    facesContext.addMessage(null, error(constraintViolation.getMessage())));
            return;
        } catch (BusinessException businessException) {
            logger.debugf("Template upload in organization %s failed with business error %s",
                    organizationId, businessException.getMessageCode());
            facesContext.addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
            return;
        } catch (RuntimeException saveFailure) {
            logger.warnf(saveFailure, "Unexpected error while saving template upload in organization %s", organizationId);
            facesContext.addMessage(null,
                    error(i18n.get("template.upload.save.failed", saveFailure.getMessage())));
            return;
        }

        logger.debugf("Uploaded template '%s' to organization %s", newName.trim(), organizationId);
        facesContext.addMessage(null, info(i18n.get("template.upload.success", newName.trim())));
        clearForm();
        reload();
    }

    public void deleteTemplate(DocumentTemplateView templateView) {
        logger.tracef("Deleting template %s on behalf of organization %s",
                templateView.getId(), organizationBean.getActiveId());
        final var facesContext = FacesContext.getCurrentInstance();

        try {
            templateManagementService.deleteTemplate(organizationBean.getActiveId(), templateView.getId());
            facesContext.addMessage(null,
                    info(i18n.get("template.delete.success", templateView.getName())));
            reload();
        } catch (BusinessException businessException) {
            logger.debugf("Deleting template %s failed with business error %s",
                    templateView.getId(), businessException.getMessageCode());
            facesContext.addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
        } catch (RuntimeException deleteFailure) {
            logger.warnf(deleteFailure, "Unexpected error while deleting template %s", templateView.getId());
            facesContext.addMessage(null,
                    error(i18n.get("template.delete.failed", templateView.getName(), deleteFailure.getMessage())));
        }
    }

    /** Used by the dataTable to render a friendly label. */
    public String labelOf(@Nullable DocumentTemplate.DocType docType) {
        if (docType == null) {
            return "-";
        }

        return switch (docType) {
            case BARANGAY_CLEARANCE -> i18n.get("template.doctype.barangay.clearance");
            case CERTIFICATE_OF_RESIDENCY -> i18n.get("template.doctype.certificate.of.residency");
            case CERTIFICATE_OF_INDIGENCY -> i18n.get("template.doctype.certificate.of.indigency");
        };
    }

    public List<SelectItem> getDocTypeOptions() {
        return Arrays.stream(DocumentTemplate.DocType.values())
                .map(docType -> new SelectItem(docType, labelOf(docType)))
                .toList();
    }

    public List<DocumentTemplateView> getTemplates() {
        return templates;
    }

    @Nullable
    public String getNewName() {
        return newName;
    }

    public void setNewName(@Nullable String newName) {
        this.newName = newName;
    }

    @Nullable
    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(@Nullable String newDescription) {
        this.newDescription = newDescription;
    }

    @Nullable
    public DocumentTemplate.DocType getNewDocType() {
        return newDocType;
    }

    public void setNewDocType(@Nullable DocumentTemplate.DocType newDocType) {
        this.newDocType = newDocType;
    }

    @Nullable
    public UploadedFile getNewFile() {
        return newFile;
    }

    public void setNewFile(@Nullable UploadedFile newFile) {
        this.newFile = newFile;
    }

    @PostConstruct
    void init() {
        reload();
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private static FacesMessage info(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }

    private void reload() {
        templates = templateManagementService.listByOrganization(organizationBean.getActiveId());
    }

    private void clearForm() {
        newName = null;
        newDescription = null;
        newDocType = null;
        newFile = null;
    }
}
