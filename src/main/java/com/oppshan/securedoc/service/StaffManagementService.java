package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Admin-driven staff CRUD scoped to a single organization. Self-service
 * staff creation lives in {@link AdminAuthService#createStaff} (called
 * from {@code AdminRegistrationBean}); this service only handles
 * activation / deactivation and role / deletion changes for existing rows.
 */
@ApplicationScoped
public class StaffManagementService {

    private final StaffRepository staffRepo;
    private final Logger logger;

    @Inject
    public StaffManagementService(StaffRepository staffRepo, Logger logger) {
        this.staffRepo = staffRepo;
        this.logger = logger;
    }

    @Transactional
    public List<StaffView> listByOrganization(@NotNull UUID organizationId) {
        logger.tracef("Listing staff in organization %s", organizationId);
        return staffRepo.listByOrganizationId(organizationId).stream()
                .map(Staff::toView)
                .toList();
    }

    @Transactional
    public void setActive(@NotNull UUID staffId, boolean active) {
        logger.tracef("Setting staff %s active flag to %s", staffId, active);
        staffRepo.setActive(staffId, active);
        logger.debugf("Updated staff %s active flag to %s", staffId, active);
    }

    @Transactional
    public void changeRole(@NotNull UUID staffId, @NotNull Staff.Role role) {
        logger.tracef("Changing staff %s role to %s", staffId, role);
        staffRepo.setRole(staffId, role);
        logger.debugf("Updated staff %s role to %s", staffId, role);
    }

    /**
     * Hard-delete. Will fail if the staff is referenced by an existing
     * request (processed_by) -- caller should surface the error and
     * consider deactivating instead.
     */
    @Transactional
    public void deleteStaff(@NotNull UUID staffId) {
        logger.tracef("Deleting staff %s", staffId);
        staffRepo.deleteById(staffId);
        logger.debugf("Deleted staff %s", staffId);
    }
}
