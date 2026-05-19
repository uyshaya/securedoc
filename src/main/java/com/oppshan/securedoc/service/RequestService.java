package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.dto.RequestSubmissionView;
import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.model.Requester;
import com.oppshan.securedoc.model.ResidentOtp;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.RequestRepository;
import com.oppshan.securedoc.repository.RequesterRepository;
import com.oppshan.securedoc.repository.ResidentOtpRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;
import org.jspecify.annotations.NonNull;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Resident-facing request flow service. Covers email OTP issuance,
 * verification (steps 1-2 of the multi-step submission wizard), and the
 * final request persistence on submit.
 */
@ApplicationScoped
public class RequestService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final ResidentOtpRepository otpRepo;
    private final RequesterRepository requesterRepo;
    private final RequestRepository requestRepo;
    private final OrganizationRepository organizationRepo;
    private final DocumentTemplateRepository templateRepo;
    private final MailService mail;
    private final Logger logger;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public RequestService(ResidentOtpRepository otpRepo,
                          RequesterRepository requesterRepo,
                          RequestRepository requestRepo,
                          OrganizationRepository organizationRepo,
                          DocumentTemplateRepository templateRepo,
                          MailService mail,
                          Logger logger) {
        this.otpRepo = otpRepo;
        this.requesterRepo = requesterRepo;
        this.requestRepo = requestRepo;
        this.organizationRepo = organizationRepo;
        this.templateRepo = templateRepo;
        this.mail = mail;
        this.logger = logger;
    }

    /**
     * Invalidates any active OTPs for this email, issues a new 6-digit code,
     * persists it, and dispatches the email.
     */
    @Transactional
    public void issueEmailOtp(@NotBlank String email) {
        logger.tracef("Issuing resident email OTP for %s", email);
        otpRepo.invalidateActive(email);

        final var otp = new ResidentOtp()
                .setEmail(email)
                .setOtpCode(generateOtpCode())
                .setExpiresAt(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        otpRepo.insertWithSession(otp);

        logger.debugf("Issued resident OTP for %s, expires at %s", email, otp.getExpiresAt());
        mail.sendResidentOtp(email, otp.getOtpCode());
    }

    /**
     * Verifies the most recent unused OTP for this email. Marks it used on
     * success or after the attempt cap; increments attempts on mismatch.
     * The entity is mutated and then merged through the active JPA session
     * via {@code updateWithSession} -- Hibernate ORM 7's Jakarta Data impl
     * runs on a {@code StatelessSession} that does not dirty-track managed
     * entities, so an explicit merge is required.
     */
    @Transactional
    public boolean verifyEmailOtp(@NotBlank String email, @NotBlank String code) {
        logger.tracef("Verifying resident email OTP for %s", email);
        final var match = otpRepo.findLatestUnused(email.trim());
        if (match.isEmpty()) {
            logger.debugf("Resident OTP verification failed for %s -- no active OTP on file", email);
            return false;
        }

        final var otp = match.get();
        boolean success;

        if (otp.isExpired() || otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            logger.debugf("Resident OTP verification failed for %s -- expired or %s attempts exhausted",
                    email, otp.getOtpAttempts());
            otp.setUsed(true);
            success = false;
        } else {
            otp.setOtpAttempts(otp.getOtpAttempts() + 1);
            if (!otp.getOtpCode().equals(code.trim())) {
                if (otp.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                    logger.debugf("Locked resident OTP for %s after %s attempts",
                            email, otp.getOtpAttempts());
                    otp.setUsed(true);
                }

                success = false;
            } else {
                logger.debugf("Resident OTP accepted for %s", email);
                otp.setUsed(true);
                success = true;
            }
        }

        otpRepo.updateWithSession(otp);
        return success;
    }

    /**
     * Persists a resident submission as one {@link Requester} row plus one
     * {@link Request} row tied to it. The request is created with status
     * PENDING and a {@code UUID.randomUUID().toString()} reference number.
     * Returns the narrow view.
     */
    @Transactional
    public RequestSubmissionView submitRequest(@Valid @NotNull RequestCreate form) {
        logger.tracef("Submitting resident request for %s in organization %s using template %s",
                form.getEmail(), form.getOrganizationId(), form.getTemplateId());
        final var organization = organizationRepo.findById(form.getOrganizationId())
                .orElseThrow(() -> BusinessException.unknownOrganization(form.getOrganizationId()));
        final var template = templateRepo.findById(form.getTemplateId())
                .orElseThrow(() -> BusinessException.unknownTemplate(form.getTemplateId()));

        final var requester = buildRequester(form);
        requesterRepo.insertWithSession(requester);

        final var request = new Request()
                .setOrganization(organization)
                .setRequester(requester)
                .setTemplate(template)
                .setReferenceNumber(UUID.randomUUID().toString())
                .setStatus(Request.Status.PENDING)
                .setPurpose(form.getPurpose())
                .setOtherPurpose(form.getOtherPurpose() == null ? null : form.getOtherPurpose().trim());
        requestRepo.insertWithSession(request);

        logger.debugf("Persisted request %s in organization %s using template %s",
                request.getReferenceNumber(), organization.getId(), template.getId());
        return request.toSubmissionView();
    }

    /**
     * Resident-facing status lookup by UUID reference number. No
     * authentication -- the reference itself is the secret. Returns a narrow
     * projection that omits requester PII so an unauthenticated caller with
     * a leaked reference can't enumerate personal data.
     */
    @Transactional
    public Optional<RequestTrackingView> lookupByReference(String referenceNumber) {
        logger.tracef("Looking up request by reference %s", referenceNumber);
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return Optional.empty();
        }

        return requestRepo.findTrackingByReferenceNumber(referenceNumber.trim());
    }

    @NonNull
    private static Requester buildRequester(RequestCreate form) {
        return new Requester()
                .setFirstName(form.getFirstName().trim())
                .setMiddleName(form.getMiddleName() == null ? null : form.getMiddleName().trim())
                .setLastName(form.getLastName().trim())
                .setEmail(form.getEmail().trim())
                .setSex(form.getSex())
                .setDateOfBirth(form.getDateOfBirth())
                .setContactNumber(form.getContactNumber() == null ? null : form.getContactNumber().trim())
                .setIdType(form.getIdType());
    }

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
