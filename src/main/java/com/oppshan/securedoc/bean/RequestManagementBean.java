package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.RequestAdminView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.service.RequestService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Backs /admin/requests.xhtml. View-scoped — the staff member is
 * viewing one paginated/filterable list per page load. Scoped to the
 * logged-in admin's active organization via {@link OrganizationBean}.
 *
 * <p>The DataTable does its own client-side pagination, sorting, and
 * filtering over {@link #getRequestList()}; this bean just loads the
 * rows and exposes label/option helpers for the filter dropdowns.
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
}
