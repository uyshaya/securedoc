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

    @Query("FROM Staff WHERE email = ?1")
    Optional<Staff> findByEmail(String email);

    @Query("FROM Staff WHERE email = ?1 AND organization.id = ?2")
    Optional<Staff> findByEmailAndOrganizationId(String email, UUID organizationId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.email = ?1 AND s.organization.id = ?2")
    long countByEmailAndOrganizationId(String email, UUID organizationId);

    @Query("FROM Staff WHERE organization.id = ?1 ORDER BY lastName, firstName")
    List<Staff> listByOrganizationId(UUID organizationId);

    @Query("UPDATE Staff SET lastLogin = ?2 WHERE id = ?1")
    int recordLogin(UUID staffId, Instant when);

    @Query("UPDATE Staff SET active = ?2 WHERE id = ?1")
    int setActive(UUID staffId, boolean active);

    @Query("UPDATE Staff SET role = ?2 WHERE id = ?1")
    int setRole(UUID staffId, Staff.Role role);
}
