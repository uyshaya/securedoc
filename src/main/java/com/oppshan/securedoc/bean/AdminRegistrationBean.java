package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

/**
 * Backs /admin/register.xhtml. Self-service staff registration:
 * collects basic fields + the applicant's barangay, validates, hashes
 * the password (via {@link AdminAuthService} → PasswordService), and
 * persists the Staff row with {@code is_active = false}.
 *
 * <p>An admin must approve the account (flip the flag) before sign-in
 * is allowed; that's enforced in {@code AdminAuthBean.signIn}.
 */
@Named("registerBean")
@RequestScoped
public class AdminRegistrationBean {

    @Inject
    private AdminAuthService authService;

    @Inject
    private SystemConfigBean system;

    @Inject
    private Logger logger;

    private static final int MIN_PASSWORD_LENGTH = 8;

    private String firstName;
    private String lastName;
    private String email;
    private Long barangayId;
    private String password;
    private String confirmPassword;

    public String register() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (isBlank(firstName) || isBlank(lastName) || isBlank(email)
                || isBlank(password) || isBlank(confirmPassword)) {
            fc.addMessage(null, error("All fields are required."));
            return null;
        }

        logger.infof("Barangay ID: %s", barangayId);
        if (barangayId == null) {
            fc.addMessage(null, error("Please select your barangay."));
            return null;
        }

        if (!password.equals(confirmPassword)) {
            fc.addMessage(null, error("Passwords do not match."));
            return null;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            fc.addMessage(null, error("Password must be at least " + MIN_PASSWORD_LENGTH + " characters."));
            return null;
        }

        if (authService.emailTakenInBarangay(email, barangayId)) {
            fc.addMessage(null, error("An account with that email already exists for the selected barangay."));
            return null;
        }

        try {
            StaffRegistrationCreate form = new StaffRegistrationCreate();
            form.setFirstName(firstName);
            form.setLastName(lastName);
            form.setEmail(email);
            form.setPassword(password);
            form.setBarangayId(barangayId);
            authService.createStaff(form);
        } catch (RuntimeException e) {
            fc.addMessage(null, error("Could not create account: " + e.getMessage()));
            return null;
        }

        // Account is persisted as inactive; an admin must approve before sign-in.
        fc.getExternalContext().getFlash().setKeepMessages(true);
        fc.addMessage(null, info(
                "Account submitted for approval. You'll be able to sign in once a barangay administrator approves your account."));
        return "/admin/login.xhtml?faces-redirect=true";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private static FacesMessage info(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Long barangayId) {
        this.barangayId = barangayId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
