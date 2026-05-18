package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    @Inject
    public StaffManagementService(StaffRepository staffRepo) {
        this.staffRepo = staffRepo;
    }

    public List<StaffView> listByOrganization(UUID organizationId) {
        return staffRepo.listByOrganizationId(organizationId).stream()
                .map(Staff::toView)
                .toList();
    }

    @Transactional
    public void setActive(UUID staffId, boolean active) {
        staffRepo.setActive(staffId, active);
    }

    @Transactional
    public void changeRole(UUID staffId, Staff.Role role) {
        staffRepo.setRole(staffId, role);
    }

    /**
     * Hard-delete. Will fail if the staff is referenced by an existing
     * request (processed_by) -- caller should surface the error and
     * consider deactivating instead.
     */
    @Transactional
    public void deleteStaff(UUID staffId) {
        staffRepo.deleteById(staffId);
    }
}
