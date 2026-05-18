package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.model.Staff.Role;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

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

    private final AdminAuthService authService;

    private final OrganizationBean organizationBean;

    private final SystemConfigBean system;

    private final I18n i18n;

    private String email;

    private String password;

    private String otpInput;

    private boolean otpSent;

    private UUID pendingStaffId;

    private UUID authenticatedId;

    private Role role;

    @Inject
    public AdminAuthBean(AdminAuthService authService,
                         OrganizationBean organizationBean,
                         SystemConfigBean system,
                         I18n i18n) {
        this.authService = authService;
        this.organizationBean = organizationBean;
        this.system = system;
        this.i18n = i18n;
    }

    protected AdminAuthBean() {
        this(null, null, null, null);
    }

    // -- step 1: email + password ----------------------------------
    public String signIn() {
        final var facesContext = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            facesContext.addMessage(null, error(i18n.get("auth.email.or.password.required")));
            return null;
        }

        final var match = authService.authenticate(email, password);
        if (match.isEmpty()) {
            facesContext.addMessage(null, error(i18n.get("auth.email.or.password.invalid")));
            return null;
        }

        final var staff = match.get();
        if (!staff.isActive()) {
            facesContext.addMessage(null,
                    error(i18n.get("auth.account.inactive", system.getOrgLabelLower())));
            return null;
        }

        authService.issueLoginOtp(staff.getId());
        this.pendingStaffId = staff.getId();
        this.password = null;
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
        if (pendingStaffId == null) {
            return;
        }

        authService.issueLoginOtp(pendingStaffId);
        this.otpInput = null;
    }

    // -- step 2: OTP -----------------------------------------------
    public String verifyOtp() {
        final var facesContext = FacesContext.getCurrentInstance();

        if (pendingStaffId == null) {
            facesContext.addMessage(null, error(i18n.get("auth.session.expired")));
            this.otpSent = false;
            return null;
        }

        if (!authService.verifyLoginOtp(pendingStaffId, otpInput)) {
            facesContext.addMessage(null, error(i18n.get("auth.otp.invalid.or.expired")));
            this.otpInput = null;
            return null;
        }

        final var match = authService.findById(pendingStaffId);
        if (match.isEmpty()) {
            facesContext.addMessage(null, error(i18n.get("auth.account.no.longer.exists")));
            cancelOtp();
            return null;
        }

        final var staff = match.get();
        authService.recordLogin(staff.getId());

        organizationBean.selectById(staff.getOrganizationId());
        this.authenticatedId = staff.getId();
        this.role = staff.getRole();
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
     * deactivated -- in that case the bean self-clears its authenticated
     * state so the caller (typically {@code AdminAuthFilter}) can
     * bounce the request to login.
     */
    public boolean refreshFromDb() {
        if (authenticatedId == null) {
            return false;
        }

        final var match = authService.findById(authenticatedId);
        if (match.isEmpty() || !match.get().isActive()) {
            this.authenticatedId = null;
            this.role = null;
            return false;
        }

        this.role = match.get().getRole();
        return true;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Role getRole() {
        return role;
    }

    public UUID getAuthenticatedId() {
        return authenticatedId;
    }

    // -- getters / setters -----------------------------------------
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
