package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.ResidentView;
import com.oppshan.securedoc.service.ResidentDirectoryService;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;
import org.primefaces.model.file.UploadedFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Backs {@code /admin/residents/residents-management.xhtml}. View-scoped:
 * the upload form + table state doesn't need to survive page navigation.
 * All ops are scoped to the logged-in admin's active organization,
 * sourced from {@link OrganizationBean}.
 */
@Named
@ViewScoped
public class ResidentDirectoryBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 6738291038472918374L;

    private final ResidentDirectoryService residentDirectoryService;

    private final OrganizationBean organizationBean;

    private final I18n i18n;

    private final Logger logger;

    private List<ResidentView> residents = List.of();

    @Nullable
    private UploadedFile newFile;

    @Inject
    public ResidentDirectoryBean(ResidentDirectoryService residentDirectoryService,
                                 OrganizationBean organizationBean,
                                 I18n i18n,
                                 Logger logger) {
        this.residentDirectoryService = residentDirectoryService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected ResidentDirectoryBean() {
        this(null, null, null, null);
    }

    public void upload() {
        logger.tracef("Importing resident CSV for organization %s", organizationBean.getActiveId());
        final var facesContext = FacesContext.getCurrentInstance();
        final var organizationId = organizationBean.getActiveId();

        if (organizationId == null) {
            facesContext.addMessage(null, error(i18n.get("residents.upload.no.active.organization")));
            return;
        }

        if (newFile == null || newFile.getSize() <= 0) {
            facesContext.addMessage(null, error(i18n.get("residents.upload.file.required")));
            return;
        }

        final var mimeType = newFile.getContentType();
        final var filename = newFile.getFileName() == null ? "" : newFile.getFileName().toLowerCase();
        final boolean acceptsType = mimeType == null
                || mimeType.equals("text/csv")
                || mimeType.equals("application/vnd.ms-excel")
                || mimeType.equals("application/octet-stream")
                || mimeType.startsWith("text/");
        if (!acceptsType && !filename.endsWith(".csv")) {
            facesContext.addMessage(null, error(i18n.get("residents.upload.csv.only")));
            return;
        }

        final ResidentDirectoryService.ImportResult result;
        try (final var stream = newFile.getInputStream()) {
            result = residentDirectoryService.replaceFromCsv(organizationId, stream);
        } catch (Exception readFailure) {
            logger.warnf(readFailure, "Failed to read resident CSV upload for organization %s", organizationId);
            facesContext.addMessage(null,
                    error(i18n.get("residents.upload.read.failed", readFailure.getMessage())));
            return;
        }

        if (!result.isSuccess()) {
            for (final var rowError : result.getErrors()) {
                facesContext.addMessage(null, error(rowError));
            }
            return;
        }

        logger.debugf("Imported %d residents into organization %s", result.getImported(), organizationId);
        facesContext.addMessage(null, info(i18n.get("residents.upload.success", result.getImported())));
        newFile = null;
        reload();
    }

    public void clearAll() {
        logger.tracef("Clearing resident directory for organization %s", organizationBean.getActiveId());
        final var facesContext = FacesContext.getCurrentInstance();
        final var organizationId = organizationBean.getActiveId();

        if (organizationId == null) {
            facesContext.addMessage(null, error(i18n.get("residents.upload.no.active.organization")));
            return;
        }

        residentDirectoryService.clearForOrganization(organizationId);
        facesContext.addMessage(null, info(i18n.get("residents.clear.success")));
        reload();
    }

    public List<ResidentView> getResidents() {
        return residents;
    }

    public int getResidentCount() {
        return residents.size();
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

    private void reload() {
        residents = residentDirectoryService.listForOrganization(organizationBean.getActiveId());
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private static FacesMessage info(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }
}
