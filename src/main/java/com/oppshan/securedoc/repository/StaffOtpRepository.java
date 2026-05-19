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

    // Hits idx_staff_otps_lookup (staff_id, otp_type, used, id) -- covers WHERE
    // + ORDER BY end-to-end. LIMIT 1 is mandatory because Optional<T> rejects
    // multi-row results, and a staff member can have multiple unused OTPs in
    // flight if they hit "resend" before invalidateActive lands.
    @Query("""
            FROM StaffOtp
            WHERE staff.id = :staffId AND otpType = :otpType AND used = FALSE
            ORDER BY id DESC
            LIMIT 1
            """)
    Optional<StaffOtp> findLatestUnused(UUID staffId, StaffOtp.Type otpType);

    @Query("""
            UPDATE StaffOtp
            SET used = TRUE
            WHERE staff.id = :staffId AND otpType = :otpType AND used = FALSE
            """)
    int invalidateActive(UUID staffId, StaffOtp.Type otpType);
}
