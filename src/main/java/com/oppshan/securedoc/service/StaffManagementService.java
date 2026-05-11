package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Admin-driven staff CRUD scoped to a single barangay. Self-service
 * staff creation lives in {@link AdminAuthService#createStaff} (called
 * from {@code AdminRegistrationBean}); this service only handles
 * activation / deactivation and role / deletion changes for existing rows.
 */
@ApplicationScoped
public class StaffManagementService {

    @Inject
    StaffRepository staffRepo;

    public List<StaffView> listByBarangay(Long barangayId) {
        return staffRepo.listByBarangayId(barangayId).stream()
                .map(Staff::toView)
                .toList();
    }

    @Transactional
    public void setActive(Long staffId, boolean active) {
        staffRepo.setActive(staffId, active);
    }

    @Transactional
    public void changeRole(Long staffId, Staff.Role role) {
        staffRepo.setRole(staffId, role);
    }

    /**
     * Hard-delete. Will fail if the staff is referenced by an existing
     * request (processed_by) — caller should surface the error and
     * consider deactivating instead.
     */
    @Transactional
    public void deleteStaff(Long staffId) {
        staffRepo.deleteById(staffId);
    }
}
