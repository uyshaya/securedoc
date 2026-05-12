package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.service.StaffManagementService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Backs /admin/staff/staff-management.xhtml. Admin-only — enforced by
 * {@code AdminAuthFilter} for the /admin/staff/* path prefix. Handles
 * activation/deactivation (i.e. approving pending registrations or
 * disabling existing accounts) and deletion. Self-service registration
 * is the only way new staff rows are created.
 */
@Named
@ViewScoped
public class StaffManagementBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 7771533201193136439L;

    @Inject
    StaffManagementService service;

    @Inject
    OrganizationBean organizationBean;

    private List<StaffView> staffList;

    @PostConstruct
    void init() {
        reload();
    }

    private void reload() {
        Long orgId = organizationBean.getActiveId();
        staffList = service.listByOrganization(orgId);
    }

    public void toggleActive(StaffView s) {
        boolean newState = !Boolean.TRUE.equals(s.getIsActive());
        service.setActive(s.getId(), newState);
        s.setIsActive(newState);
        FacesContext.getCurrentInstance().addMessage(null,
                info(s.getFullName() + " " + (newState ? "activated." : "deactivated.")));
    }

    /**
     * Persists the new role for the row whose dropdown was changed.
     * The selection has already updated {@code s.role} via JSF binding
     * before this listener fires.
     */
    public void changeRole(StaffView s) {
        service.changeRole(s.getId(), s.getRole());
        FacesContext.getCurrentInstance().addMessage(null,
                info(s.getFullName() + " role set to "
                        + (s.getRole() == Staff.Role.ADMIN ? "Admin." : "Staff.")));
    }

    public List<SelectItem> getRoleOptions() {
        return List.of(
                new SelectItem(Staff.Role.STAFF, "Staff"),
                new SelectItem(Staff.Role.ADMIN, "Admin")
        );
    }

    public void deleteStaff(StaffView s) {
        try {
            service.deleteStaff(s.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    info(s.getFullName() + " deleted."));
            reload();
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    error("Could not delete " + s.getFullName()
                            + " — they may have processed requests on file. Deactivate instead."));
        }
    }

    private static FacesMessage error(String s) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, s, null);
    }

    private static FacesMessage info(String s) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, s, null);
    }

    public List<StaffView> getStaffList() {
        return staffList;
    }
}