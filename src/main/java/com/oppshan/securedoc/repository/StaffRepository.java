package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.Staff;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository
        extends CrudRepository<Staff, UUID>, StatefulWriteRepository<Staff> {

    @Query("""
            FROM Staff
            WHERE email = :email
            """)
    Optional<Staff> findByEmail(String email);

    // Hits uc_staff_organization_email UNIQUE (organization_id, email).
    @Query("""
            FROM Staff
            WHERE email = :email AND organization.id = :organizationId
            """)
    Optional<Staff> findByEmailAndOrganizationId(String email, UUID organizationId);

    @Query("""
            SELECT COUNT(s)
            FROM Staff s
            WHERE s.email = :email AND s.organization.id = :organizationId
            """)
    long countByEmailAndOrganizationId(String email, UUID organizationId);

    // Hits idx_staff_organization_lastname_firstname (organization_id, last_name, first_name)
    // -- covers WHERE + ORDER BY without a filesort.
    @Query("""
            FROM Staff
            WHERE organization.id = :organizationId
            ORDER BY lastName, firstName
            """)
    List<Staff> listByOrganizationId(UUID organizationId);

    @Query("""
            UPDATE Staff
            SET lastLogin = :when
            WHERE id = :staffId
            """)
    int recordLogin(UUID staffId, Instant when);

    @Query("""
            UPDATE Staff
            SET active = :active
            WHERE id = :staffId
            """)
    int setActive(UUID staffId, boolean active);

    @Query("""
            UPDATE Staff
            SET role = :role
            WHERE id = :staffId
            """)
    int setRole(UUID staffId, Staff.Role role);
}
