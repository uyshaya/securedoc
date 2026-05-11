package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.StaffOtp;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface StaffOtpRepository extends BasicRepository<StaffOtp, Long> {

    @Query("FROM StaffOtp WHERE staff.id = ?1 AND otpType = ?2 AND isUsed = false ORDER BY id DESC")
    Optional<StaffOtp> findLatestUnused(Long staffId, StaffOtp.Type type);

    @Query("UPDATE StaffOtp SET isUsed = true WHERE staff.id = ?1 AND otpType = ?2 AND isUsed = false")
    int invalidateActive(Long staffId, StaffOtp.Type type);
}
