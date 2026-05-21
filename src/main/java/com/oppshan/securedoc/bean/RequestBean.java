package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.service.RequestService;
import com.oppshan.securedoc.service.TemplateManagementService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;
import org.jboss.logging.Logger;
import org.primefaces.model.file.UploadedFile;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Backs the multi-scene resident document-request flow on
 * /user/request.xhtml. Session-scoped because the user walks through
 * scenes (landing -> email -> otp -> details -> review -> confirm) via JS
 * scene transitions, and the picked organization/template selections
 * must persist across them. Anonymous flow -- no login required.
 *
 * <p>JSF-bound scenes: landing (org autocomplete + cert-type dropdown),
 * email (send OTP), otp (verify OTP), details (field-level
 * {@code required="true"} plus {@link #summarizeDetailsValidation}).
 * Review and confirm scenes are still JS-only stubs until the
 * persistence + verification-token backend lands.
 */
@Named("requestBean")
@SessionScoped
public class RequestBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 8273419837461829374L;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final long MAX_ID_IMAGE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_ID_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/pdf"
    );

    private final SystemConfigBean system;

    private final TemplateManagementService templateService;

    private final RequestService requestService;

    private final I18n i18n;

    private final Logger logger;

    @Nullable
    private OrganizationView selectedOrganization;

    @Nullable
    private UUID selectedTemplateId;

    private List<DocumentTemplateView> availableTemplates = List.of();

    /**
     * Resident's email -- bound on the email-verification scene.
     */
    @Nullable
    private String email;

    /**
     * Joined 6-digit OTP code, synced from the 6-cell DOM by request.js.
     */
    @Nullable
    private String otpInput;

    /**
     * Flag flipped on after a successful OTP verify, gating the next scene.
     */
    private boolean emailVerified;

    // -- Details-scene fields -------------------------------------
    @Nullable
    private String firstName;

    @Nullable
    private String middleName;

    @Nullable
    private String lastName;

    @Nullable
    private LocalDate dateOfBirth;

    @Nullable
    private String sex;

    @Nullable
    private String contactNumber;

    @Nullable
    private String purpose;

    @Nullable
    private String otherPurpose;

    @Nullable
    private String idType;

    /**
     * Bound by JSF during a multipart submit of the details form (the
     * {@code p:fileUpload} on the details scene). Lives for the request that
     * actually carries the multipart; we read its bytes into
     * {@link #idImageData} immediately so the session-scoped state survives
     * the round-trip to the review scene without holding the temp file
     * reference.
     */
    @Nullable
    private UploadedFile uploadedIdFile;

    @Nullable
    private byte[] idImageData;

    @Nullable
    private String idImageMimeType;

    @Nullable
    private String idImageFileName;

    /**
     * Reference number for the just-submitted request, surfaced on
     * the confirmation scene. Generated server-side by
     * {@link #submitRequest()} so the value the resident sees matches
     * what will live in the database once persistence lands.
     */
    @Nullable
    private String submittedReference;

    // -- Track-scene fields ---------------------------------------
    /**
     * Reference number bound on the track scene; the resident types
     * (or pastes from their confirmation email) the UUID here.
     */
    @Nullable
    private String trackReference;

    /**
     * Populated by {@link #trackRequest()} on a successful lookup;
     * null otherwise. The track scene's result panel renders only
     * when this is non-null.
     */
    @Nullable
    private RequestTrackingView trackedResult;

    /**
     * True after a failed lookup so the scene can flag "not found"
     * inline. Separate from {@code trackedResult == null} because the
     * default state (resident hasn't searched yet) shouldn't show
     * an error.
     */
    private boolean trackingNotFound;

    @Inject
    public RequestBean(SystemConfigBean system,
                       TemplateManagementService templateService,
                       RequestService requestService,
                       I18n i18n,
                       Logger logger) {
        this.system = system;
        this.templateService = templateService;
        this.requestService = requestService;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected RequestBean() {
        this(null, null, null, null, null);
    }

    /** Called by the autocomplete's {@code completeMethod} as the resident types. */
    public List<OrganizationView> completeOrganization(String query) {
        return system.searchOrganizations(query);
    }

    /**
     * AJAX listener fired when the resident picks an organization. Loads
     * that org's active templates so the cert-type dropdown can populate
     * without a full page reload.
     */
    public void onOrganizationSelected() {
        logger.tracef("Resident picked organization %s; loading its templates",
                selectedOrganization == null ? null : selectedOrganization.getId());
        selectedTemplateId = null;

        availableTemplates = templateService.listByOrganization(
                selectedOrganization == null ? null : selectedOrganization.getId());
    }

    /**
     * Server-side validation invoked by the landing-scene "Proceed" button.
     * Returns null in all cases -- scene advancement is driven by the
     * button's {@code oncomplete} callback, which checks
     * {@code args.validationFailed} before calling {@code goTo('p-email')}.
     */
    public String proceedFromLanding() {
        logger.tracef("Validating landing scene with organization %s and template %s",
                selectedOrganization == null ? null : selectedOrganization.getId(), selectedTemplateId);
        final var facesContext = FacesContext.getCurrentInstance();

        if (selectedOrganization == null || selectedOrganization.getId() == null) {
            logger.debugf("Rejected landing scene -- no organization selected");
            facesContext.addMessage(null,
                    error(i18n.get("request.landing.organization.required", system.getOrgLabelLower())));
            facesContext.validationFailed();
            return null;
        }

        if (selectedTemplateId == null) {
            logger.debugf("Rejected landing scene -- no certificate type chosen");
            facesContext.addMessage(null, error(i18n.get("request.landing.certificate.required")));
            facesContext.validationFailed();
            return null;
        }

        return null;
    }

    /**
     * Email-scene "Send code" action. Validates format server-side,
     * delegates persistence + mail dispatch to {@link RequestService}.
     * Returns null in all cases: the email scene's JS advances to the
     * OTP scene via {@code oncomplete} on success.
     */
    public String sendOtp() {
        logger.tracef("Resident requested OTP for email %s", email);
        final var facesContext = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank()) {
            logger.debugf("Rejected OTP request -- email is blank");
            facesContext.addMessage(null, error(i18n.get("request.email.required")));
            facesContext.validationFailed();
            return null;
        }

        final var trimmedEmail = email.trim();
        if (!EMAIL_REGEX.matcher(trimmedEmail).matches()) {
            logger.debugf("Rejected OTP request for %s -- email format is invalid", trimmedEmail);
            facesContext.addMessage(null, error(i18n.get("request.email.invalid")));
            facesContext.validationFailed();
            return null;
        }

        try {
            requestService.issueEmailOtp(trimmedEmail);
            this.email = trimmedEmail;
            this.emailVerified = false;
            this.otpInput = null;
        } catch (BusinessException businessException) {
            logger.debugf("OTP request for %s failed with business error %s",
                    trimmedEmail, businessException.getMessageCode());
            facesContext.addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
            facesContext.validationFailed();
        } catch (RuntimeException sendFailure) {
            logger.warnf(sendFailure, "Unexpected error while sending resident OTP to %s", trimmedEmail);
            facesContext.addMessage(null,
                    error(i18n.get("request.email.send.failed", sendFailure.getMessage())));
            facesContext.validationFailed();
        }

        return null;
    }

    /**
     * OTP-scene action. Reads the 6-digit code synced from the cell
     * inputs (via request.js -> {@code otpInput}) and asks the service
     * to verify against the most recent unused OTP for the email.
     * Returns null in all cases: scene advancement is driven by the
     * button's {@code oncomplete} on success.
     */
    public String verifyOtp() {
        logger.tracef("Verifying resident OTP for %s", email);
        final var facesContext = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank()) {
            logger.debugf("Rejected OTP verification -- session expired before resident entered code");
            facesContext.addMessage(null, error(i18n.get("request.otp.session.expired")));
            facesContext.validationFailed();
            return null;
        }

        if (otpInput == null || otpInput.isBlank()) {
            logger.debugf("Rejected OTP verification for %s -- code missing from input", email);
            facesContext.addMessage(null, error(i18n.get("request.otp.required")));
            facesContext.validationFailed();
            return null;
        }

        final var verified = requestService.verifyEmailOtp(email, otpInput);
        if (!verified) {
            logger.debugf("OTP verification failed for %s -- code invalid or expired", email);
            facesContext.addMessage(null, error(i18n.get("request.otp.invalid.or.expired")));
            facesContext.validationFailed();
            this.otpInput = null;
            return null;
        }

        logger.debugf("Resident OTP accepted for %s", email);
        this.emailVerified = true;
        this.otpInput = null;
        return null;
    }

    /**
     * Form-level {@code postValidate} hook wired from
     * {@code <f:event type="postValidate">} on {@code detailsForm}. Fires
     * after JSF has run {@code required="true"} validation on every child
     * input. The form's {@code <p:messages globalOnly="true">} panel skips
     * per-component messages, so without this the resident gets no
     * feedback when a required field is empty. Surface a single summary
     * in the global panel: the failing fields still flag themselves
     * inline if/when per-field {@code <p:message>} components are added.
     */
    public void summarizeDetailsValidation(ComponentSystemEvent event) {
        final var facesContext = FacesContext.getCurrentInstance();
        if (facesContext.isValidationFailed()) {
            facesContext.addMessage(null, error(i18n.get("request.details.required.missing")));
        }
    }

    /**
     * Details-scene "Review & Submit" action. Validation is handled
     * by JSF {@code required="true"} on the field tags; when invoked,
     * all required fields are populated. Returns null so the
     * button's {@code oncomplete} JS can transition the scene.
     */
    public String proceedToReview() {
        logger.tracef("Validating details scene for purpose %s", purpose);
        final var facesContext = FacesContext.getCurrentInstance();

        // Cross-field check: "Other" purpose requires the free-text reason.
        if ("other".equals(purpose) && (otherPurpose == null || otherPurpose.isBlank())) {
            logger.debugf("Rejected details scene -- purpose is 'other' but the free-text reason is empty");
            facesContext.addMessage(null, error(i18n.get("request.details.other.purpose.required")));
            facesContext.validationFailed();
        }

        if (!consumeUploadedIdFile(facesContext)) {
            return null;
        }

        if (idImageData == null || idImageData.length == 0) {
            logger.debugf("Rejected details scene -- valid ID upload is missing");
            facesContext.addMessage(null, error(i18n.get("request.details.upload.required")));
            facesContext.validationFailed();
        }

        return null;
    }

    /**
     * Reads the multipart {@code p:fileUpload} payload into {@link #idImageData}
     * if a new file was attached on this submit. Validates size + MIME type
     * and surfaces a per-issue error in the global panel. Returns false when
     * a new upload was attempted but rejected -- the caller stops further
     * checks in that case so the resident sees one focused message.
     */
    private boolean consumeUploadedIdFile(FacesContext facesContext) {
        if (uploadedIdFile == null || uploadedIdFile.getSize() <= 0) {
            return true;
        }

        try {
            if (uploadedIdFile.getSize() > MAX_ID_IMAGE_BYTES) {
                logger.debugf("Rejected ID upload -- %s bytes exceeds %s byte cap",
                        uploadedIdFile.getSize(), MAX_ID_IMAGE_BYTES);
                facesContext.addMessage(null, error(i18n.get("request.details.upload.too.large")));
                facesContext.validationFailed();
                return false;
            }

            final var contentType = uploadedIdFile.getContentType();
            if (contentType == null
                    || !ALLOWED_ID_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                logger.debugf("Rejected ID upload -- unsupported mime type %s", contentType);
                facesContext.addMessage(null, error(i18n.get("request.details.upload.invalid.type")));
                facesContext.validationFailed();
                return false;
            }

            try {
                this.idImageData = uploadedIdFile.getContent();
            } catch (RuntimeException readFailure) {
                logger.warnf(readFailure, "Failed to read resident ID upload for %s", email);
                facesContext.addMessage(null,
                        error(i18n.get("request.details.upload.read.failed", readFailure.getMessage())));
                facesContext.validationFailed();
                return false;
            }

            this.idImageMimeType = contentType;
            this.idImageFileName = uploadedIdFile.getFileName();
            return true;
        } finally {
            this.uploadedIdFile = null;
        }
    }

    /**
     * Review-scene "Submit Request" action. Persists the requester +
     * request rows via {@link RequestService#submitRequest} and exposes
     * the generated UUID reference on {@link #getSubmittedReference()}
     * so the confirmation scene can render it. Returns null: the
     * button's {@code oncomplete} handles the scene transition.
     *
     * <p>Cryptographic signing (the issued {@code documents} row) lands
     * in a later phase: that happens when staff approves the request,
     * not at submission time.
     */
    public String submitRequest() {
        logger.tracef("Submitting resident request for %s in organization %s using template %s",
                email,
                selectedOrganization == null ? null : selectedOrganization.getId(),
                selectedTemplateId);
        final var facesContext = FacesContext.getCurrentInstance();
        final var form = buildRequestCreate();

        try {
            final var submitted = requestService.submitRequest(form);
            this.submittedReference = submitted.getReferenceNumber();
            logger.debugf("Resident %s submitted request with reference %s", email, submittedReference);
        } catch (ConstraintViolationException violation) {
            logger.debugf("Submission rejected for %s -- %s constraint violation(s)",
                    email, violation.getConstraintViolations().size());
            violation.getConstraintViolations().forEach(constraintViolation ->
                    facesContext.addMessage(null, error(constraintViolation.getMessage())));
            facesContext.validationFailed();
        } catch (BusinessException businessException) {
            logger.debugf("Submission failed for %s with business error %s",
                    email, businessException.getMessageCode());
            facesContext.addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
            facesContext.validationFailed();
        } catch (RuntimeException submitFailure) {
            logger.warnf(submitFailure, "Unexpected error while submitting resident request for %s", email);
            facesContext.addMessage(null,
                    error(i18n.get("request.submit.failed", submitFailure.getMessage())));
            facesContext.validationFailed();
        }

        return null;
    }

    /**
     * Track-scene "Check" action. Looks up the request by reference
     * number (anonymous: the UUID itself is the secret) and surfaces
     * either {@link #trackedResult} for the result panel or
     * {@link #trackingNotFound} for the inline error notice.
     * Returns null since the scene re-renders in place.
     */
    public String trackRequest() {
        logger.tracef("Tracking request by reference %s", trackReference);
        this.trackedResult = null;
        this.trackingNotFound = false;

        if (trackReference == null || trackReference.isBlank()) {
            return null;
        }

        requestService.lookupByReference(trackReference)
                .ifPresentOrElse(
                        view -> this.trackedResult = view,
                        () -> this.trackingNotFound = true
                );
        return null;
    }

    public List<SelectItem> getSexOptions() {
        return List.of(
                new SelectItem("M", i18n.get("request.sex.male")),
                new SelectItem("F", i18n.get("request.sex.female"))
        );
    }

    public List<SelectItem> getPurposeOptions() {
        return List.of(
                new SelectItem("employment", i18n.get("request.purpose.employment")),
                new SelectItem("travel", i18n.get("request.purpose.travel")),
                new SelectItem("school", i18n.get("request.purpose.school")),
                new SelectItem("loan", i18n.get("request.purpose.loan")),
                new SelectItem("legal", i18n.get("request.purpose.legal")),
                new SelectItem("insurance", i18n.get("request.purpose.insurance")),
                new SelectItem("personal", i18n.get("request.purpose.personal")),
                new SelectItem("other", i18n.get("request.purpose.other"))
        );
    }

    public List<SelectItem> getIdTypeOptions() {
        return List.of(
                new SelectItem(i18n.get("request.id.philippine.passport")),
                new SelectItem(i18n.get("request.id.drivers.license")),
                new SelectItem(i18n.get("request.id.philsys")),
                new SelectItem(i18n.get("request.id.voters")),
                new SelectItem(i18n.get("request.id.sss.gsis")),
                new SelectItem(i18n.get("request.id.prc")),
                new SelectItem(i18n.get("request.id.postal")),
                new SelectItem(i18n.get("request.id.barangay"))
        );
    }

    @Nullable
    public OrganizationView getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(@Nullable OrganizationView selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
    }

    @Nullable
    public UUID getSelectedTemplateId() {
        return selectedTemplateId;
    }

    public void setSelectedTemplateId(@Nullable UUID selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    public List<DocumentTemplateView> getAvailableTemplates() {
        return availableTemplates;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getOtpInput() {
        return otpInput;
    }

    public void setOtpInput(@Nullable String otpInput) {
        this.otpInput = otpInput;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@Nullable String lastName) {
        this.lastName = lastName;
    }

    @Nullable
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Nullable
    public String getSex() {
        return sex;
    }

    public void setSex(@Nullable String sex) {
        this.sex = sex;
    }

    @Nullable
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(@Nullable String contactNumber) {
        this.contactNumber = contactNumber;
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
    }

    @Nullable
    public String getOtherPurpose() {
        return otherPurpose;
    }

    public void setOtherPurpose(@Nullable String otherPurpose) {
        this.otherPurpose = otherPurpose;
    }

    @Nullable
    public String getIdType() {
        return idType;
    }

    public void setIdType(@Nullable String idType) {
        this.idType = idType;
    }

    @Nullable
    public UploadedFile getUploadedIdFile() {
        return uploadedIdFile;
    }

    public void setUploadedIdFile(@Nullable UploadedFile uploadedIdFile) {
        this.uploadedIdFile = uploadedIdFile;
    }

    @Nullable
    public String getIdImageFileName() {
        return idImageFileName;
    }

    @Nullable
    public String getSubmittedReference() {
        return submittedReference;
    }

    @Nullable
    public String getTrackReference() {
        return trackReference;
    }

    public void setTrackReference(@Nullable String trackReference) {
        this.trackReference = trackReference;
    }

    @Nullable
    public RequestTrackingView getTrackedResult() {
        return trackedResult;
    }

    public boolean isTrackingNotFound() {
        return trackingNotFound;
    }

    /**
     * Placeholder text for the cert-type dropdown, reflecting the org-selection state.
     */
    public String getCertTypePlaceholder() {
        if (selectedOrganization == null) {
            return i18n.get("request.landing.certificate.placeholder.no.org", system.getOrgLabelLower());
        }

        if (availableTemplates.isEmpty()) {
            return i18n.get("request.landing.certificate.placeholder.none", system.getOrgLabelLower());
        }

        return i18n.get("request.landing.certificate.placeholder.select");
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private RequestCreate buildRequestCreate() {
        return new RequestCreate()
                .setOrganizationId(selectedOrganization == null ? null : selectedOrganization.getId())
                .setTemplateId(selectedTemplateId)
                .setEmail(email)
                .setFirstName(firstName)
                .setMiddleName(middleName)
                .setLastName(lastName)
                .setDateOfBirth(dateOfBirth)
                .setSex(sex)
                .setContactNumber(contactNumber)
                .setIdType(idType)
                .setIdImageData(idImageData)
                .setPurpose(purpose)
                .setOtherPurpose(otherPurpose);
    }
}
