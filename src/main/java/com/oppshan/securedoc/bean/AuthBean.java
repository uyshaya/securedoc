package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.service.AuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * Drives the admin sign-in flow on /admin/login
 * <p>
 * email + password -> email otp -> /admin/dashboard
 */
@Named
@SessionScoped
public class AuthBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    transient AuthService authService;

    @Inject
    BarangayBean barangayBean;

    private String email;
    private String password;
    private String otpInput;

    private boolean otpSent;
    private Long pendingStaffId;   // set after step 1, cleared after step 2
    private Long authenticatedId;  // set after successful OTP verify

    // ── step 1: email + password ──────────────────────────────────

    public String signIn() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            fc.addMessage(null, error("Email or password is required."));
            return null;
        }

        Optional<Staff> match = authService.findByEmail(email);
        if (match.isEmpty() || !authService.verifyPassword(match.get(), password)) {
            fc.addMessage(null, error("Email or password is invalid."));
            return null;
        }

        Staff staff = match.get();
        if (!Boolean.TRUE.equals(staff.getIsActive())) {
            fc.addMessage(null, error("Account is inactive. Contact your barangay administrator."));
            return null;
        }

        authService.issueLoginOtp(staff);
        this.pendingStaffId = staff.getId();
        this.password = null;          // clear plaintext from session
        this.otpInput = null;
        this.otpSent = true;
        return null;
    }

    public void cancelOtp() {
        this.otpSent = false;
        this.otpInput = null;
        this.pendingStaffId = null;
    }

    public void resendOtp() {
        if (pendingStaffId == null) return;
        authService.findById(pendingStaffId).ifPresent(authService::issueLoginOtp);
        this.otpInput = null;
    }

    // ── step 2: OTP ───────────────────────────────────────────────

    public String verifyOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (pendingStaffId == null) {
            fc.addMessage(null, error("Session expired. Please sign in again."));
            this.otpSent = false;
            return null;
        }

        if (!authService.verifyLoginOtp(pendingStaffId, otpInput)) {
            fc.addMessage(null, error("Invalid or expired code. Please try again."));
            this.otpInput = null;
            return null;
        }

        Optional<Staff> match = authService.findById(pendingStaffId);
        if (match.isEmpty()) {
            fc.addMessage(null, error("Account no longer exists."));
            cancelOtp();
            return null;
        }

        Staff staff = match.get();
        authService.recordLogin(staff.getId());

        // Promote: set the active barangay for this session and remember
        // who we are. Forget the pending step.
        barangayBean.setActive(staff.getBarangay());
        this.authenticatedId = staff.getId();
        this.pendingStaffId = null;
        this.otpSent = false;
        this.otpInput = null;

        return "/admin/dashboard.xhtml?faces-redirect=true";
    }

    public String signOut() {
        this.email = null;
        this.password = null;
        this.otpInput = null;
        this.otpSent = false;
        this.pendingStaffId = null;
        this.authenticatedId = null;
        barangayBean.clear();
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/admin/login.xhtml?faces-redirect=true";
    }

    public boolean isAuthenticated() {
        return authenticatedId != null;
    }

    // ── getters / setters ─────────────────────────────────────────
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtpInput() {
        return otpInput;
    }

    public void setOtpInput(String otpInput) {
        this.otpInput = otpInput;
    }

    public boolean isOtpSent() {
        return otpSent;
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }
}
