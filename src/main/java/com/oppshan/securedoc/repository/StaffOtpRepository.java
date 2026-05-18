package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.StaffOtp;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffOtpRepository
        extends CrudRepository<StaffOtp, UUID>, StatefulWriteRepository<StaffOtp> {

    @Query("FROM StaffOtp WHERE staff.id = ?1 AND otpType = ?2 AND used = false ORDER BY id DESC")
    Optional<StaffOtp> findLatestUnused(UUID staffId, StaffOtp.Type type);

    @Query("UPDATE StaffOtp SET used = true WHERE staff.id = ?1 AND otpType = ?2 AND used = false")
    int invalidateActive(UUID staffId, StaffOtp.Type type);
}
