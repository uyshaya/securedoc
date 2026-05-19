package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.service.StaffManagementService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

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
    private final Logger logger;

    private List<StaffView> staffList;

    @Inject
    public StaffManagementBean(StaffManagementService staffManagementService,
                               OrganizationBean organizationBean,
                               I18n i18n,
                               Logger logger) {
        this.staffManagementService = staffManagementService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected StaffManagementBean() {
        this(null, null, null, null);
    }

    public void toggleActive(StaffView staff) {
        logger.tracef("Toggling active flag for staff %s (currently %s)", staff.getId(), staff.isActive());
        final var nextActiveState = !staff.isActive();
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
        logger.tracef("Changing staff %s role to %s", staff.getId(), staff.getRole());
        staffManagementService.changeRole(staff.getId(), staff.getRole());

        final var roleLabelKey = staff.getRole() == Staff.Role.ADMIN ? "staff.role.admin" : "staff.role.staff";
        FacesContext.getCurrentInstance().addMessage(null,
                info(i18n.get("staff.role.changed", staff.getFullName(), i18n.get(roleLabelKey))));
    }

    public List<SelectItem> getRoleOptions() {
        return List.of(
                new SelectItem(Staff.Role.STAFF, i18n.get("staff.role.staff")),
                new SelectItem(Staff.Role.ADMIN, i18n.get("staff.role.admin"))
        );
    }

    public void deleteStaff(StaffView staff) {
        logger.tracef("Deleting staff %s", staff.getId());
        try {
            staffManagementService.deleteStaff(staff.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    info(i18n.get("staff.deleted", staff.getFullName())));
            reload();
        } catch (BusinessException businessException) {
            logger.debugf("Deleting staff %s failed with business error %s",
                    staff.getId(), businessException.getMessageCode());
            FacesContext.getCurrentInstance().addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
        } catch (RuntimeException deleteFailure) {
            logger.warnf(deleteFailure, "Unexpected error while deleting staff %s", staff.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    error(i18n.get("staff.delete.failed", staff.getFullName())));
        }
    }

    public List<StaffView> getStaffList() {
        return staffList;
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
        final var activeOrganizationId = organizationBean.getActiveId();
        staffList = staffManagementService.listByOrganization(activeOrganizationId);
    }
}
