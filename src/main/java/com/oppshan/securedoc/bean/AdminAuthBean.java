package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.service.AdminAuthService;
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
@Named("authBean")
@SessionScoped
public class AdminAuthBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1052852303714482777L;

    @Inject
    transient AdminAuthService authService;

    @Inject
    OrganizationBean organizationBean;

    @Inject
    SystemConfigBean system;

    private String email;
    private String password;
    private String otpInput;

    private boolean otpSent;
    private Long pendingStaffId;   // set after step 1, cleared after step 2
    private Long authenticatedId;  // set after successful OTP verify
    private Staff.Role role;       // cached at successful OTP verify
    private String fullName;       // cached for sidebar profile chip

    // ── step 1: email + password ──────────────────────────────────
    public String signIn() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            fc.addMessage(null, error("Email or password is required."));
            return null;
        }

        Optional<StaffView> match = authService.authenticate(email, password);
        if (match.isEmpty()) {
            fc.addMessage(null, error("Email or password is invalid."));
            return null;
        }

        StaffView staff = match.get();
        if (!Boolean.TRUE.equals(staff.getIsActive())) {
            fc.addMessage(null, error("Account is inactive. Contact your " + system.getOrgLabelLower() + " administrator."));
            return null;
        }

        authService.issueLoginOtp(staff.getId());
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
        authService.issueLoginOtp(pendingStaffId);
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

        Optional<StaffView> match = authService.findById(pendingStaffId);
        if (match.isEmpty()) {
            fc.addMessage(null, error("Account no longer exists."));
            cancelOtp();
            return null;
        }

        StaffView staff = match.get();
        authService.recordLogin(staff.getId());

        // Promote: set the active organization for this session and remember
        // who we are. Forget the pending step.
        organizationBean.selectById(staff.getOrganizationId());
        this.authenticatedId = staff.getId();
        this.role = staff.getRole();
        this.fullName = staff.getFullName();
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
        this.role = null;
        this.fullName = null;
        organizationBean.clear();
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/admin/login.xhtml?faces-redirect=true";
    }

    public boolean isAuthenticated() {
        return authenticatedId != null;
    }

    /**
     * Re-reads the persisted Staff row and refreshes the cached role.
     * Returns false if the account no longer exists or has been
     * deactivated — in that case the bean self-clears its authenticated
     * state so the caller (typically {@code AdminAuthFilter}) can
     * bounce the request to login.
     */
    public boolean refreshFromDb() {
        if (authenticatedId == null) return false;
        Optional<StaffView> match = authService.findById(authenticatedId);
        if (match.isEmpty() || !Boolean.TRUE.equals(match.get().getIsActive())) {
            this.authenticatedId = null;
            this.role = null;
            this.fullName = null;
            return false;
        }
        this.role = match.get().getRole();
        this.fullName = match.get().getFullName();
        return true;
    }

    public boolean isAdmin() {
        return role == Staff.Role.ADMIN;
    }

    public Staff.Role getRole() {
        return role;
    }

    public Long getAuthenticatedId() {
        return authenticatedId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRoleLabel() {
        if (role == null) return "";
        return role == Staff.Role.ADMIN ? "Admin" : "Staff";
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
