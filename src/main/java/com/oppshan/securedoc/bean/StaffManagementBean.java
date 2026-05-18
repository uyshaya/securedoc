package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
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
 * Backs /admin/staff/staff-management.xhtml. Admin-only -- enforced by
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

    private final StaffManagementService staffManagementService;
    private final OrganizationBean organizationBean;
    private final I18n i18n;

    private List<StaffView> staffList;

    @Inject
    public StaffManagementBean(StaffManagementService staffManagementService,
                               OrganizationBean organizationBean,
                               I18n i18n) {
        this.staffManagementService = staffManagementService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
    }

    protected StaffManagementBean() {
        this(null, null, null);
    }

    @PostConstruct
    void init() {
        reload();
    }

    private void reload() {
        final var activeOrganizationId = organizationBean.getActiveId();
        staffList = staffManagementService.listByOrganization(activeOrganizationId);
    }

    public void toggleActive(StaffView staff) {
        final var nextActiveState = !Boolean.TRUE.equals(staff.isActive());
        staffManagementService.setActive(staff.getId(), nextActiveState);
        staff.setActive(nextActiveState);

        final var messageKey = nextActiveState ? "staff.activated" : "staff.deactivated";
        FacesContext.getCurrentInstance().addMessage(null,
                info(i18n.get(messageKey, staff.getFullName())));
    }

    /**
     * Persists the new role for the row whose dropdown was changed.
     * The selection has already updated {@code staff.role} via JSF binding
     * before this listener fires.
     */
    public void changeRole(StaffView staff) {
        staffManagementService.changeRole(staff.getId(), staff.getRole());

        final var roleLabelKey = staff.getRole() == Staff.Role.ADMIN ? "staff.role.admin" : "staff.role.staff";
        FacesContext.getCurrentInstance().addMessage(null,
                info(i18n.get("staff.role.changed", staff.getFullName(), i18n.get(roleLabelKey))));
    }

    public List<SelectItem> getRoleOptions() {
        return List.of(
                new SelectItem(Staff.Role.STAFF, "Staff"),
                new SelectItem(Staff.Role.ADMIN, "Admin")
        );
    }

    public void deleteStaff(StaffView staff) {
        try {
            staffManagementService.deleteStaff(staff.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    info(i18n.get("staff.deleted", staff.getFullName())));
            reload();
        } catch (RuntimeException exception) {
            FacesContext.getCurrentInstance().addMessage(null,
                    error(i18n.get("staff.delete.failed", staff.getFullName())));
        }
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private static FacesMessage info(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }

    public List<StaffView> getStaffList() {
        return staffList;
    }
}
