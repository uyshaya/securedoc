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

    @Query("FROM Staff WHERE email = ?1 AND barangay.id = ?2")
    Optional<Staff> findByEmailAndBarangayId(String email, Long barangayId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.email = ?1 AND s.barangay.id = ?2")
    long countByEmailAndBarangayId(String email, Long barangayId);

    @Query("FROM Staff WHERE barangay.id = ?1 ORDER BY lastName, firstName")
    List<Staff> listByBarangayId(Long barangayId);

    @Query("UPDATE Staff SET lastLogin = ?2 WHERE id = ?1")
    int recordLogin(Long staffId, LocalDateTime when);

    @Query("UPDATE Staff SET isActive = ?2 WHERE id = ?1")
    int setActive(Long staffId, boolean active);

    @Query("UPDATE Staff SET role = ?2 WHERE id = ?1")
    int setRole(Long staffId, Staff.Role role);
}
