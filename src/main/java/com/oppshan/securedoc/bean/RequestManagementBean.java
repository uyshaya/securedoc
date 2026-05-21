package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.RequestAdminView;
import com.oppshan.securedoc.dto.RequestDetailView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Request;
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
    OrganizationBean organizationBean;

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
     * Stub -- the actual status transition + processed_by stamping is
     * deferred until the issuance flow is built. Leaving the action wired
     * to a no-op so the button can be styled and tested in place.
     */
    public void approve() {
        // TODO: advance status (PENDING -> UNDER_REVIEW etc.), stamp processed_by, refresh list.
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
