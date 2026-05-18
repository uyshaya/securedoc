package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.dto.RequestSubmissionView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.service.RequestService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Backs the multi-scene resident document-request flow on
 * /user/request.xhtml. Session-scoped because the user walks through
 * scenes (landing → email → otp → details → review → confirm) via JS
 * scene transitions, and the picked organization/template selections
 * must persist across them. Anonymous flow — no login required.
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

    @Inject
    SystemConfigBean system;

    @Inject
    DocumentTemplateRepository templateRepo;

    @Inject
    RequestService requestService;

    private OrganizationView selectedOrganization;
    private Long selectedTemplateId;
    private List<DocumentTemplateView> availableTemplates = List.of();

    /**
     * Resident's email — bound on the email-verification scene.
     */
    private String email;

    /**
     * Joined 6-digit OTP code, synced from the 6-cell DOM by request.js.
     */
    private String otpInput;

    /**
     * Flag flipped on after a successful OTP verify, gating the next scene.
     */
    private boolean emailVerified;

    // ── Details-scene fields ─────────────────────────────────────
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String sex;
    private String contactNumber;
    private String purpose;
    private String otherPurpose;
    private String idType;

    /**
     * Reference number for the just-submitted request, surfaced on
     * the confirmation scene. Generated server-side by
     * {@link #submitRequest()} so the value the resident sees matches
     * what will live in the database once persistence lands.
     */
    private String submittedReference;

    /**
     * Called by the autocomplete's {@code completeMethod} as the resident types.
     */
    public List<OrganizationView> completeOrganization(String query) {
        return system.searchOrganizations(query);
    }

    /**
     * AJAX listener fired when the resident picks an organization. Loads
     * that org's active templates so the cert-type dropdown can populate
     * without a full page reload.
     */
    public void onOrganizationSelected() {
        selectedTemplateId = null;
        if (selectedOrganization == null || selectedOrganization.getId() == null) {
            availableTemplates = List.of();
            return;
        }
        availableTemplates = templateRepo.listActiveByOrganizationId(selectedOrganization.getId()).stream()
                .map(DocumentTemplate::toView)
                .toList();
    }

    /**
     * Server-side validation invoked by the landing-scene "Proceed" button.
     * Returns null in all cases — scene advancement is driven by the
     * button's {@code oncomplete} callback, which checks
     * {@code args.validationFailed} before calling {@code goTo('p-email')}.
     */
    public String proceedFromLanding() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (selectedOrganization == null || selectedOrganization.getId() == null) {
            fc.addMessage(null, error("Please select your " + system.getOrgLabelLower() + "."));
            fc.validationFailed();
            return null;
        }
        if (selectedTemplateId == null) {
            fc.addMessage(null, error("Please select a certificate type."));
            fc.validationFailed();
            return null;
        }
        return null;
    }

    /**
     * Email-scene "Send code" action. Validates format server-side,
     * delegates persistence + mail dispatch to {@link RequestService}.
     * Returns null in all cases — the email scene's JS advances to the
     * OTP scene via {@code oncomplete} on success.
     */
    public String sendOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (email == null || email.isBlank()) {
            fc.addMessage(null, error("Email is required."));
            fc.validationFailed();
            return null;
        }
        String trimmed = email.trim();
        if (!EMAIL_REGEX.matcher(trimmed).matches()) {
            fc.addMessage(null, error("Please enter a valid email address."));
            fc.validationFailed();
            return null;
        }
        try {
            requestService.issueEmailOtp(trimmed);
            this.email = trimmed;   // canonicalize for downstream scenes
            this.emailVerified = false;  // any prior verify is now stale
            this.otpInput = null;
        } catch (RuntimeException sendFailure) {
            fc.addMessage(null, error("Could not send code: " + sendFailure.getMessage()));
            fc.validationFailed();
        }
        return null;
    }

    /**
     * OTP-scene action. Reads the 6-digit code synced from the cell
     * inputs (via request.js → {@code otpInput}) and asks the service
     * to verify against the most recent unused OTP for the email.
     * Returns null in all cases — scene advancement is driven by the
     * button's {@code oncomplete} on success.
     */
    public String verifyOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (email == null || email.isBlank()) {
            fc.addMessage(null, error("Session expired. Please re-enter your email."));
            fc.validationFailed();
            return null;
        }
        if (otpInput == null || otpInput.isBlank()) {
            fc.addMessage(null, error("Please enter the 6-digit code."));
            fc.validationFailed();
            return null;
        }
        boolean verified = requestService.verifyEmailOtp(email, otpInput);
        if (!verified) {
            fc.addMessage(null, error("Invalid or expired code. Please try again."));
            fc.validationFailed();
            this.otpInput = null;
            return null;
        }
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
     * in the global panel — the failing fields still flag themselves
     * inline if/when per-field {@code <p:message>} components are added.
     */
    public void summarizeDetailsValidation(ComponentSystemEvent event) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc.isValidationFailed()) {
            fc.addMessage(null, error("One or more required fields are missing."));
        }
    }

    /**
     * Details-scene "Review & Submit" action. Validation is handled
     * by JSF {@code required="true"} on the field tags; when invoked,
     * all required fields are populated. Returns null so the
     * button's {@code oncomplete} JS can transition the scene.
     */
    public String proceedToReview() {
        // Cross-field check: "Other" purpose requires the free-text reason.
        if ("other".equals(purpose) && (otherPurpose == null || otherPurpose.isBlank())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    error("Please describe your purpose."));
            FacesContext.getCurrentInstance().validationFailed();
        }
        return null;
    }

    /**
     * Review-scene "Submit Request" action. Persists the requester +
     * request rows via {@link RequestService#submitRequest} and exposes
     * the generated UUID reference on {@link #getSubmittedReference()}
     * so the confirmation scene can render it. Returns null — the
     * button's {@code oncomplete} handles the scene transition.
     *
     * <p>Cryptographic signing (the issued {@code documents} row) lands
     * in a later phase — that happens when staff approves the request,
     * not at submission time.
     */
    public String submitRequest() {
        FacesContext fc = FacesContext.getCurrentInstance();
        RequestCreate form = getRequestCreate();
        try {
            RequestSubmissionView submitted = requestService.submitRequest(form);
            this.submittedReference = submitted.getReferenceNumber();
        } catch (RuntimeException submitFailure) {
            fc.addMessage(null, error("Could not submit request: " + submitFailure.getMessage()));
            fc.validationFailed();
        }
        return null;
    }

    private @NonNull RequestCreate getRequestCreate() {
        RequestCreate form = new RequestCreate();
        form.setOrganizationId(selectedOrganization == null ? null : selectedOrganization.getId());
        form.setTemplateId(selectedTemplateId);
        form.setEmail(email);
        form.setFirstName(firstName);
        form.setMiddleName(middleName);
        form.setLastName(lastName);
        form.setDateOfBirth(dateOfBirth);
        form.setSex(sex);
        form.setContactNumber(contactNumber);
        form.setIdType(idType);
        form.setPurpose(purpose);
        form.setOtherPurpose(otherPurpose);
        return form;
    }

    public List<SelectItem> getSexOptions() {
        return List.of(
                new SelectItem("M", "Male"),
                new SelectItem("F", "Female")
        );
    }

    public List<SelectItem> getPurposeOptions() {
        return List.of(
                new SelectItem("employment", "Employment / Job Application"),
                new SelectItem("travel", "Travel / Passport / Visa"),
                new SelectItem("school", "School Enrollment / Scholarship"),
                new SelectItem("loan", "Bank / Loan Application"),
                new SelectItem("legal", "Legal / Court Proceedings"),
                new SelectItem("insurance", "Insurance / Benefits"),
                new SelectItem("personal", "Personal Record / Reference"),
                new SelectItem("other", "Other")
        );
    }

    public List<SelectItem> getIdTypeOptions() {
        return List.of(
                new SelectItem("Philippine Passport"),
                new SelectItem("Driver's License"),
                new SelectItem("PhilSys (National ID)"),
                new SelectItem("Voter's ID / COMELEC Card"),
                new SelectItem("SSS / GSIS Card"),
                new SelectItem("PRC ID"),
                new SelectItem("Postal ID"),
                new SelectItem("Barangay ID")
        );
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    public OrganizationView getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(OrganizationView selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
    }

    public Long getSelectedTemplateId() {
        return selectedTemplateId;
    }

    public void setSelectedTemplateId(Long selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    public List<DocumentTemplateView> getAvailableTemplates() {
        return availableTemplates;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpInput() {
        return otpInput;
    }

    public void setOtpInput(String otpInput) {
        this.otpInput = otpInput;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getOtherPurpose() {
        return otherPurpose;
    }

    public void setOtherPurpose(String otherPurpose) {
        this.otherPurpose = otherPurpose;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getSubmittedReference() {
        return submittedReference;
    }

    /**
     * Placeholder text for the cert-type dropdown, reflecting the org-selection state.
     */
    public String getCertTypePlaceholder() {
        if (selectedOrganization == null) {
            return "Select your " + system.getOrgLabelLower() + " first…";
        }
        if (availableTemplates.isEmpty()) {
            return "No certificate types available for this " + system.getOrgLabelLower();
        }
        return "Select a certificate type…";
    }
}
