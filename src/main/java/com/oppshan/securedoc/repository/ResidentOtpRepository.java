package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.ResidentOtp;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResidentOtpRepository
        extends CrudRepository<ResidentOtp, UUID>, StatefulWriteRepository<ResidentOtp> {

    // Hits idx_resident_otp_lookup (email, used, id) -- covers WHERE + ORDER BY
    // end-to-end. LIMIT 1 is mandatory because Optional<T> rejects multi-row
    // results, and an email can legitimately have multiple unused OTPs in
    // flight if the resident hits "resend" before invalidateActive lands.
    @Query("""
            FROM ResidentOtp
            WHERE email = :email AND used = FALSE
            ORDER BY id DESC
            LIMIT 1
            """)
    Optional<ResidentOtp> findLatestUnused(String email);

    @Query("""
            UPDATE ResidentOtp
            SET used = TRUE
            WHERE email = :email AND used = FALSE
            """)
    int invalidateActive(String email);
}
