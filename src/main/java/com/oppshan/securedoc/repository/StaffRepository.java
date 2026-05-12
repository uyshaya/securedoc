package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Staff;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends BasicRepository<Staff, Long> {

    @Query("FROM Staff WHERE email = ?1")
    Optional<Staff> findByEmail(String email);

    @Query("FROM Staff WHERE email = ?1 AND organization.id = ?2")
    Optional<Staff> findByEmailAndOrganizationId(String email, Long organizationId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.email = ?1 AND s.organization.id = ?2")
    long countByEmailAndOrganizationId(String email, Long organizationId);

    @Query("FROM Staff WHERE organization.id = ?1 ORDER BY lastName, firstName")
    List<Staff> listByOrganizationId(Long organizationId);

    @Query("UPDATE Staff SET lastLogin = ?2 WHERE id = ?1")
    int recordLogin(Long staffId, LocalDateTime when);

    @Query("UPDATE Staff SET isActive = ?2 WHERE id = ?1")
    int setActive(Long staffId, boolean active);

    @Query("UPDATE Staff SET role = ?2 WHERE id = ?1")
    int setRole(Long staffId, Staff.Role role);
}
