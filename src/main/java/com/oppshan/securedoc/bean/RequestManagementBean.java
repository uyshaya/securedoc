package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.RequestAdminView;
import com.oppshan.securedoc.dto.RequestDetailView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.service.DocumentService;
import com.oppshan.securedoc.service.RequestService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Nullable;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Backs /admin/requests.xhtml: loads requests for the active
 * organization, exposes filter options for the DataTable, and holds
 * the detail-sidebar state ({@link #selectedRequest},
 * {@link #rejectNote}).
 */
@Named
@ViewScoped
public class RequestManagementBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 8273619482736182937L;

    @Inject
    RequestService service;

    @Inject
    DocumentService documentService;

    @Inject
    OrganizationBean organizationBean;

    @Inject
    AdminAuthBean adminAuthBean;

    private List<RequestAdminView> requestList;

    @Nullable
    private RequestDetailView selectedRequest;

    @Nullable
    private String rejectNote;

    @PostConstruct
    void init() {
        requestList = service.listForOrganization(organizationBean.getActiveId());
    }

    public List<SelectItem> getStatusOptions() {
        return Arrays.stream(Request.Status.values())
                .map(status -> new SelectItem(status, status.getLabel()))
                .toList();
    }

    public List<SelectItem> getDocTypeOptions() {
        return Arrays.stream(DocumentTemplate.DocType.values())
                .map(type -> new SelectItem(type, type.getLabel()))
                .toList();
    }

    public List<RequestAdminView> getRequestList() {
        return requestList;
    }

    /**
     * Loads the detail projection for {@code row} and populates
     * {@link #selectedRequest} so the sidebar can render. Called from
     * the per-row "view" command button on the requests table.
     */
    public void loadDetail(RequestAdminView row) {
        if (row == null || row.getId() == null) {
            return;
        }
        selectedRequest = service.getDetail(row.getId(), organizationBean.getActiveId()).orElse(null);
        rejectNote = null;
    }

    public void closeDetail() {
        selectedRequest = null;
        rejectNote = null;
    }

    /**
     * Approves the selected request: generates the PDF from the request's
     * template, persists a {@code document} row tied 1:1 to the request,
     * stamps {@code processed_by} with the acting admin, and flips the
     * request to COMPLETED. All in one transaction inside
     * {@link DocumentService#issueForRequest}, so the table never reflects
     * a half-issued state. PKI signing + QR + verifier portal land in
     * later tickets.
     */
    public void approve() {
        if (selectedRequest == null) {
            return;
        }
        final var issued = documentService.issueForRequest(
                selectedRequest.getId(),
                organizationBean.getActiveId(),
                adminAuthBean.getAuthenticatedId());
        if (issued) {
            requestList = service.listForOrganization(organizationBean.getActiveId());
        }
        closeDetail();
    }

    /**
     * Stub -- {@link #rejectNote} is captured and will be persisted as
     * {@code request_note} once the state-change service method is wired.
     */
    public void reject() {
        // TODO: set status REJECTED, persist rejectNote as request_note, stamp processed_by, refresh list.
    }

    @Nullable
    public RequestDetailView getSelectedRequest() {
        return selectedRequest;
    }

    public boolean isDetailVisible() {
        return selectedRequest != null;
    }

    public void setDetailVisible(boolean visible) {
        if (!visible) {
            closeDetail();
        }
    }

    @Nullable
    public String getRejectNote() {
        return rejectNote;
    }

    public void setRejectNote(@Nullable String rejectNote) {
        this.rejectNote = rejectNote;
    }
}
