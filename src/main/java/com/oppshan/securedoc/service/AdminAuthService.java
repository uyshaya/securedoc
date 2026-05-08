package com.oppshan.securedoc.service;

import com.oppshan.securedoc.model.Barangay;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.model.StaffOtp;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class AdminAuthService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    @Inject
    EntityManager em;

    @Inject
    PasswordService passwordService;

    @Inject
    MailService mail;

    private final SecureRandom random = new SecureRandom();

    // ── Staff lookup ─────────────────────────────────────────────

    public Optional<Staff> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(Staff.class, id));
    }

    public Optional<Staff> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(em.createQuery(
                            "SELECT s FROM Staff s WHERE s.email = :email", Staff.class)
                    .setParameter("email", email.trim())
                    .setMaxResults(1)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Staff> findByEmailAndBarangay(String email, Long barangayId) {
        if (email == null || email.isBlank() || barangayId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(em.createQuery(
                            "SELECT s FROM Staff s WHERE s.email = :email AND s.barangay.id = :bid",
                            Staff.class)
                    .setParameter("email", email.trim())
                    .setParameter("bid", barangayId)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    // ── Step 1: password ─────────────────────────────────────────

    public boolean verifyPassword(Staff staff, String plaintext) {
        return staff != null && passwordService.verify(plaintext, staff.getPasswordHash());
    }

    // ── Step 2: OTP issue + verify ───────────────────────────────

    @Transactional
    public StaffOtp issueLoginOtp(Staff staff) {
        // Invalidate any prior live login OTPs so only the latest is valid.
        em.createQuery("UPDATE StaffOtp o SET o.isUsed = true " +
                        "WHERE o.staff.id = :id AND o.otpType = :type AND o.isUsed = false")
                .setParameter("id", staff.getId())
                .setParameter("type", StaffOtp.Type.LOGIN)
                .executeUpdate();

        StaffOtp staffOtp = new StaffOtp();
        staffOtp.setStaff(staff);
        staffOtp.setOtpCode(generateOtpCode());
        staffOtp.setOtpType(StaffOtp.Type.LOGIN);
        staffOtp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        em.persist(staffOtp);

        mail.sendStaffOtp(staff.getEmail(), staffOtp.getOtpCode());
        return staffOtp;
    }

    /**
     * Verifies the most recent unused login OTP for the given staff.
     * On success the OTP is marked used; failure increments attempts
     * and locks the OTP after {@link #MAX_OTP_ATTEMPTS}.
     */
    @Transactional
    public boolean verifyLoginOtp(Long staffId, String code) {
        if (staffId == null || code == null || code.isBlank()) return false;

        StaffOtp otp;
        try {
            otp = em.createQuery(
                            "SELECT o FROM StaffOtp o " +
                                    "WHERE o.staff.id = :id AND o.otpType = :type AND o.isUsed = false " +
                                    "ORDER BY o.id DESC", StaffOtp.class)
                    .setParameter("id", staffId)
                    .setParameter("type", StaffOtp.Type.LOGIN)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return false;
        }

        if (otp.isExpired() || otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            otp.setIsUsed(true);
            return false;
        }

        otp.setOtpAttempts(otp.getOtpAttempts() + 1);

        if (!otp.getOtpCode().equals(code.trim())) {
            if (otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                otp.setIsUsed(true);
            }
            return false;
        }

        otp.setIsUsed(true);
        return true;
    }

    // ── Registration ─────────────────────────────────────────────

    public boolean emailTakenInBarangay(String email, Long barangayId) {
        if (email == null || barangayId == null) {
            return false;
        }
        Long count = em.createQuery(
                        "SELECT COUNT(s) FROM Staff s WHERE s.email = :email AND s.barangay.id = :bid",
                        Long.class)
                .setParameter("email", email.trim())
                .setParameter("bid", barangayId)
                .getSingleResult();
        return count != null && count > 0;
    }

    /**
     * Creates a self-service staff registration. The row is persisted with
     * {@code is_active = false} so a barangay administrator must approve
     * the account (flip the flag) before {@link AuthBean#signIn()} will
     * accept the credentials.
     */
    @Transactional
    public Staff createStaff(String firstName, String lastName, String email,
                             String plaintextPassword, Long barangayId) {
        Barangay barangay = em.find(Barangay.class, barangayId);
        if (barangay == null) {
            throw new IllegalArgumentException("Unknown barangay: " + barangayId);
        }
        Staff staff = new Staff();
        staff.setFirstName(firstName.trim());
        staff.setLastName(lastName.trim());
        staff.setEmail(email.trim());
        staff.setPasswordHash(passwordService.hash(plaintextPassword));
        staff.setBarangay(barangay);
        staff.setRole(Staff.Role.STAFF);
        staff.setIsActive(Boolean.FALSE);   // pending admin approval
        em.persist(staff);
        return staff;
    }

    @Transactional
    public void recordLogin(Long staffId) {
        em.createQuery("UPDATE Staff s SET s.lastLogin = :now WHERE s.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", staffId)
                .executeUpdate();
    }

    // ── helpers ──────────────────────────────────────────────────

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
