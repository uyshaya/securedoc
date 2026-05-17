package com.oppshan.securedoc.service;

import com.oppshan.securedoc.model.ResidentOtp;
import com.oppshan.securedoc.repository.ResidentOtpRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resident-facing request flow service. Covers email OTP issuance and
 * verification (steps 1–2 of the multi-step submission wizard);
 * request persistence lands in a later phase.
 */
@ApplicationScoped
public class RequestService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    @Inject
    ResidentOtpRepository otpRepo;

    @Inject
    MailService mail;

    private final SecureRandom random = new SecureRandom();

    /**
     * Invalidates any active OTPs for this email, issues a new 6-digit
     * code, persists it, and dispatches the email.
     */
    @Transactional
    public void issueEmailOtp(String email) {
        otpRepo.invalidateActive(email);

        ResidentOtp otp = new ResidentOtp();
        otp.setEmail(email);
        otp.setOtpCode(generateOtpCode());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otpRepo.save(otp);

        mail.sendResidentOtp(email, otp.getOtpCode());
    }

    /**
     * Verifies the most recent unused OTP for this email. Marks it used
     * on success or after the attempt cap; increments attempts on
     * mismatch. Entity is explicitly {@code save()}d because Jakarta
     * Data repos run on a StatelessSession without dirty tracking.
     */
    @Transactional
    public boolean verifyEmailOtp(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        Optional<ResidentOtp> match = otpRepo.findLatestUnused(email.trim());
        if (match.isEmpty()) {
            return false;
        }
        ResidentOtp otp = match.get();

        boolean success;
        if (otp.isExpired() || otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            otp.setIsUsed(true);
            success = false;
        } else {
            otp.setOtpAttempts(otp.getOtpAttempts() + 1);
            if (!otp.getOtpCode().equals(code.trim())) {
                if (otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                    otp.setIsUsed(true);
                }
                success = false;
            } else {
                otp.setIsUsed(true);
                success = true;
            }
        }
        otpRepo.save(otp);
        return success;
    }

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
