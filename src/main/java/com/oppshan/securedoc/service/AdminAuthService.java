package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.dto.StaffRegistrationView;
import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Barangay;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.model.StaffOtp;
import com.oppshan.securedoc.repository.BarangayRepository;
import com.oppshan.securedoc.repository.StaffOtpRepository;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class AdminAuthService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    @Inject
    StaffRepository staffRepo;

    @Inject
    StaffOtpRepository otpRepo;

    @Inject
    BarangayRepository barangayRepo;

    @Inject
    PasswordService passwordService;

    @Inject
    MailService mail;

    private final SecureRandom random = new SecureRandom();

    // ── Staff lookup ─────────────────────────────────────────────

    public Optional<StaffView> findById(Long id) {
        if (id == null) return Optional.empty();
        return staffRepo.findById(id).map(Staff::toView);
    }

    // ── Step 1: email + password ─────────────────────────────────

    /**
     * Finds the staff by email and verifies the password in one call so
     * the entity (and its hash) never leaves the service. Returns the
     * DTO on success, empty otherwise. Does NOT check {@code isActive} —
     * caller decides how to message that case.
     */
    public Optional<StaffView> authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        Optional<Staff> match = staffRepo.findByEmail(email);
        if (match.isEmpty() || !passwordService.verify(password, match.get().getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(match.get().toView());
    }

    // ── Step 2: OTP issue + verify ───────────────────────────────

    @Transactional
    public void issueLoginOtp(Long staffId) {
        Optional<Staff> match = staffRepo.findById(staffId);
        if (match.isEmpty()) return;
        Staff staff = match.get();

        otpRepo.invalidateActive(staffId, StaffOtp.Type.LOGIN);

        StaffOtp staffOtp = new StaffOtp();
        staffOtp.setStaff(staff);
        staffOtp.setOtpCode(generateOtpCode());
        staffOtp.setOtpType(StaffOtp.Type.LOGIN);
        staffOtp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otpRepo.save(staffOtp);

        mail.sendStaffOtp(staff.getEmail(), staffOtp.getOtpCode());
    }

    /**
     * Verifies the most recent unused login OTP for the given staff.
     * On success the OTP is marked used; failure increments attempts
     * and locks the OTP after {@link #MAX_OTP_ATTEMPTS}. The entity is
     * mutated and then explicitly {@code save()}d because the generated
     * Jakarta Data impl uses a {@code StatelessSession} — there is no
     * dirty-tracking auto-flush on managed entities.
     */
    @Transactional
    public boolean verifyLoginOtp(Long staffId, String code) {
        if (staffId == null || code == null || code.isBlank()) return false;

        Optional<StaffOtp> match = otpRepo.findLatestUnused(staffId, StaffOtp.Type.LOGIN);
        if (match.isEmpty()) return false;
        StaffOtp otp = match.get();

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

    // ── Registration ─────────────────────────────────────────────

    public boolean emailTakenInBarangay(String email, Long barangayId) {
        return staffRepo.countByEmailAndBarangayId(email, barangayId) > 0;
    }

    /**
     * Creates a self-service staff registration. The row is persisted with
     * {@code is_active = false} so a barangay administrator must approve
     * the account before sign-in is accepted.
     */
    @Transactional
    public StaffRegistrationView createStaff(StaffRegistrationCreate form) {
        Barangay barangay = barangayRepo.findById(form.getBarangayId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown barangay: " + form.getBarangayId()));
        Staff staff = new Staff();
        staff.setFirstName(form.getFirstName().trim());
        staff.setLastName(form.getLastName().trim());
        staff.setEmail(form.getEmail().trim());
        staff.setPasswordHash(passwordService.hash(form.getPassword()));
        staff.setBarangay(barangay);
        staff.setRole(Staff.Role.STAFF);
        staff.setIsActive(Boolean.FALSE);   // pending admin approval
        staffRepo.save(staff);
        return staff.toRegistrationView();
    }

    @Transactional
    public void recordLogin(Long staffId) {
        staffRepo.recordLogin(staffId, LocalDateTime.now());
    }

    // ── helpers ──────────────────────────────────────────────────

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}