package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.dto.StaffRegistrationView;
import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.model.StaffOtp;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.StaffOtpRepository;
import com.oppshan.securedoc.repository.StaffRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AdminAuthService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final StaffRepository staffRepo;
    private final StaffOtpRepository otpRepo;
    private final OrganizationRepository organizationRepo;
    private final PasswordService passwordService;
    private final MailService mail;
    private final Logger logger;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public AdminAuthService(StaffRepository staffRepo,
                            StaffOtpRepository otpRepo,
                            OrganizationRepository organizationRepo,
                            PasswordService passwordService,
                            MailService mail,
                            Logger logger) {
        this.staffRepo = staffRepo;
        this.otpRepo = otpRepo;
        this.organizationRepo = organizationRepo;
        this.passwordService = passwordService;
        this.mail = mail;
        this.logger = logger;
    }

    // -- Staff lookup ---------------------------------------------

    @Transactional
    public Optional<StaffView> findById(@NotNull UUID id) {
        logger.tracef("Looking up staff by id %s", id);
        return staffRepo.findById(id).map(Staff::toView);
    }

    // -- Step 1: email + password ---------------------------------

    /**
     * Finds the staff by email and verifies the password in one call so
     * the entity (and its hash) never leaves the service. Returns the
     * DTO on success, empty otherwise. Does NOT check {@code isActive} --
     * caller decides how to message that case.
     */
    @Transactional
    public Optional<StaffView> authenticate(@NotBlank String email, @NotBlank String password) {
        logger.tracef("Authenticating staff with email %s", email);
        final var match = staffRepo.findByEmail(email);
        if (match.isEmpty() || !passwordService.verify(password, match.get().getPasswordHash())) {
            logger.debugf("Sign-in failed for %s -- email or password invalid", email);
            return Optional.empty();
        }

        logger.debugf("Sign-in credentials accepted for %s (staff %s)", email, match.get().getId());
        return Optional.of(match.get().toView());
    }

    // -- Step 2: OTP issue + verify -------------------------------

    @Transactional
    public void issueLoginOtp(@NotNull UUID staffId) {
        logger.tracef("Issuing login OTP for staff %s", staffId);
        final var match = staffRepo.findById(staffId);
        if (match.isEmpty()) {
            logger.debugf("Skipped issuing login OTP -- staff %s not found", staffId);
            return;
        }

        final var staff = match.get();
        otpRepo.invalidateActive(staffId, StaffOtp.Type.LOGIN);

        final var staffOtp = new StaffOtp()
                .setStaff(staff)
                .setOtpCode(generateOtpCode())
                .setOtpType(StaffOtp.Type.LOGIN)
                .setExpiresAt(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        otpRepo.insertWithSession(staffOtp);

        logger.debugf("Issued login OTP for staff %s, expires at %s", staffId, staffOtp.getExpiresAt());
        mail.sendStaffOtp(staff.getEmail(), staffOtp.getOtpCode());
    }

    /**
     * Verifies the most recent unused login OTP for the given staff.
     * On success the OTP is marked used; failure increments attempts
     * and locks the OTP after {@link #MAX_OTP_ATTEMPTS}. The entity is
     * mutated and then merged through the active JPA session via
     * {@code updateWithSession} -- the Hibernate-generated Jakarta Data
     * impl runs on a {@code StatelessSession} that does not dirty-track
     * managed entities, so an explicit merge is required.
     */
    @Transactional
    public boolean verifyLoginOtp(@NotNull UUID staffId, @NotBlank String code) {
        logger.tracef("Verifying login OTP for staff %s", staffId);
        final var match = otpRepo.findLatestUnused(staffId, StaffOtp.Type.LOGIN);
        if (match.isEmpty()) {
            logger.debugf("Login OTP verification failed for staff %s -- no active OTP on file", staffId);
            return false;
        }

        final var otp = match.get();
        boolean success;

        if (otp.isExpired() || otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            logger.debugf("Login OTP verification failed for staff %s -- expired or %s attempts exhausted",
                    staffId, otp.getOtpAttempts());
            otp.setUsed(true);
            success = false;
        } else {
            otp.setOtpAttempts(otp.getOtpAttempts() + 1);
            if (!otp.getOtpCode().equals(code.trim())) {
                if (otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                    logger.debugf("Locked login OTP for staff %s after %s attempts",
                            staffId, otp.getOtpAttempts());
                    otp.setUsed(true);
                }

                success = false;
            } else {
                logger.debugf("Login OTP accepted for staff %s", staffId);
                otp.setUsed(true);
                success = true;
            }
        }

        otpRepo.updateWithSession(otp);
        return success;
    }

    // -- Registration ---------------------------------------------

    @Transactional
    public boolean emailTakenInOrganization(@NotBlank String email, @NotNull UUID organizationId) {
        logger.tracef("Checking whether email %s is already taken in organization %s", email, organizationId);
        return staffRepo.countByEmailAndOrganizationId(email, organizationId) > 0;
    }

    /**
     * Creates a self-service staff registration. The row is persisted with
     * {@code is_active = false} so an organization administrator must
     * approve the account before sign-in is accepted.
     */
    @Transactional
    public StaffRegistrationView createStaff(@Valid @NotNull StaffRegistrationCreate form) {
        logger.tracef("Creating staff registration for %s in organization %s",
                form.getEmail(), form.getOrganizationId());
        final var organization = organizationRepo.findById(form.getOrganizationId())
                .orElseThrow(() -> BusinessException.unknownOrganization(form.getOrganizationId()));
        final var staff = new Staff()
                .setFirstName(form.getFirstName().trim())
                .setLastName(form.getLastName().trim())
                .setEmail(form.getEmail().trim())
                .setPasswordHash(passwordService.hash(form.getPassword()))
                .setOrganization(organization)
                .setRole(Staff.Role.STAFF)
                .setActive(Boolean.FALSE);
        staffRepo.insertWithSession(staff);

        logger.debugf("Registered staff %s (%s) in organization %s, pending approval",
                staff.getId(), staff.getEmail(), organization.getId());
        return staff.toRegistrationView();
    }

    @Transactional
    public void recordLogin(@NotNull UUID staffId) {
        logger.tracef("Recording last-login timestamp for staff %s", staffId);
        staffRepo.recordLogin(staffId, Instant.now());
    }

    // -- helpers --------------------------------------------------

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
