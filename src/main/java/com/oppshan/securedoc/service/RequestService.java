package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.dto.RequestSubmissionView;
import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
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
import org.jspecify.annotations.NonNull;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Resident-facing request flow service. Covers email OTP issuance,
 * verification (steps 1–2 of the multi-step submission wizard), and
 * the final request persistence on submit.
 */
@ApplicationScoped
public class RequestService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    @Inject
    ResidentOtpRepository otpRepo;

    @Inject
    RequesterRepository requesterRepo;

    @Inject
    RequestRepository requestRepo;

    @Inject
    OrganizationRepository organizationRepo;

    @Inject
    DocumentTemplateRepository templateRepo;

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

    /**
     * Persists a resident submission as one {@link Requester} row plus
     * one {@link Request} row tied to it. The request is created with
     * status PENDING and a {@code UUID.randomUUID().toString()}
     * reference number. Returns the narrow view.
     */
    @Transactional
    public RequestSubmissionView submitRequest(RequestCreate form) {
        Organization organization = organizationRepo.findById(form.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + form.getOrganizationId()));
        DocumentTemplate template = templateRepo.findById(form.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + form.getTemplateId()));

        Requester requester = getRequester(form);
        requesterRepo.save(requester);

        Request request = new Request();
        request.setOrganization(organization);
        request.setRequester(requester);
        request.setTemplate(template);
        request.setReferenceNumber(UUID.randomUUID().toString());
        request.setStatus(Request.Status.PENDING);
        request.setPurpose(form.getPurpose());
        request.setOtherPurpose(form.getOtherPurpose() == null ? null : form.getOtherPurpose().trim());
        requestRepo.save(request);

        return request.toSubmissionView();
    }

    /**
     * Resident-facing status lookup by UUID reference number. No
     * authentication — the reference itself is the secret. Returns a
     * narrow projection that omits requester PII so an unauthenticated
     * caller with a leaked reference can't enumerate personal data.
     */
    public Optional<RequestTrackingView> lookupByReference(String referenceNumber) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return Optional.empty();
        }
        return requestRepo.findTrackingByReferenceNumber(referenceNumber.trim());
    }

    private static @NonNull Requester getRequester(RequestCreate form) {
        Requester requester = new Requester();
        requester.setFirstName(form.getFirstName().trim());
        requester.setMiddleName(form.getMiddleName() == null ? null : form.getMiddleName().trim());
        requester.setLastName(form.getLastName().trim());
        requester.setEmail(form.getEmail().trim());
        requester.setSex(form.getSex());
        requester.setDateOfBirth(form.getDateOfBirth());
        requester.setContactNumber(form.getContactNumber() == null ? null : form.getContactNumber().trim());
        requester.setIdType(form.getIdType());
        return requester;
    }

    private String generateOtpCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
