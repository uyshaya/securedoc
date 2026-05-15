package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.ResidentOtp;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface ResidentOtpRepository extends BasicRepository<ResidentOtp, Long> {

    @Query("FROM ResidentOtp WHERE email = ?1 AND isUsed = false ORDER BY id DESC")
    Optional<ResidentOtp> findLatestUnused(String email);

    @Query("UPDATE ResidentOtp SET isUsed = true WHERE email = ?1 AND isUsed = false")
    int invalidateActive(String email);
}
