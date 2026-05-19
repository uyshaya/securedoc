package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.model.Staff.Role;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

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

    private final Logger logger;

    private String email;

    private String password;

    private String otpInput;

    private boolean otpSent;
    private String fullName;       // cached for sidebar profile chip

    private UUID pendingStaffId;

    private UUID authenticatedId;

    private Role role;

    @Inject
    public AdminAuthBean(AdminAuthService authService,
                         OrganizationBean organizationBean,
                         SystemConfigBean system,
                         I18n i18n,
                         Logger logger) {
        this.authService = authService;
        this.organizationBean = organizationBean;
        this.system = system;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected AdminAuthBean() {
        this(null, null, null, null, null);
    }

    // -- step 1: email + password ----------------------------------
    public String signIn() {
        logger.tracef("Sign-in step 1 starting for %s", email);
        final var facesContext = FacesContext.getCurrentInstance();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            logger.debugf("Rejected sign-in for %s -- email or password missing", email);
            facesContext.addMessage(null, error(i18n.get("auth.email.or.password.required")));
            return null;
        }

        final var match = authService.authenticate(email, password);
        if (match.isEmpty()) {
            logger.debugf("Sign-in failed for %s -- credentials invalid", email);
            facesContext.addMessage(null, error(i18n.get("auth.email.or.password.invalid")));
            return null;
        }

        final var staff = match.get();
        if (!staff.isActive()) {
            logger.debugf("Rejected sign-in for staff %s -- account is inactive", staff.getId());
            facesContext.addMessage(null,
                    error(i18n.get("auth.account.inactive", system.getOrgLabelLower())));
            return null;
        }

        logger.debugf("Requesting login OTP for staff %s", staff.getId());
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
        logger.tracef("Resending login OTP for pending staff %s", pendingStaffId);
        if (pendingStaffId == null) {
            return;
        }

        authService.issueLoginOtp(pendingStaffId);
        this.otpInput = null;
    }

    // -- step 2: OTP -----------------------------------------------
    public String verifyOtp() {
        logger.tracef("Verifying login OTP for pending staff %s", pendingStaffId);
        final var facesContext = FacesContext.getCurrentInstance();

        if (pendingStaffId == null) {
            logger.debugf("Rejected OTP verification -- session expired before code was entered");
            facesContext.addMessage(null, error(i18n.get("auth.session.expired")));
            this.otpSent = false;
            return null;
        }

        if (!authService.verifyLoginOtp(pendingStaffId, otpInput)) {
            logger.debugf("OTP verification failed for staff %s -- code invalid or expired", pendingStaffId);
            facesContext.addMessage(null, error(i18n.get("auth.otp.invalid.or.expired")));
            this.otpInput = null;
            return null;
        }

        final var match = authService.findById(pendingStaffId);
        if (match.isEmpty()) {
            logger.debugf("OTP verified but staff %s no longer exists in the database", pendingStaffId);
            facesContext.addMessage(null, error(i18n.get("auth.account.no.longer.exists")));
            cancelOtp();
            return null;
        }

        final var staff = match.get();
        authService.recordLogin(staff.getId());

        organizationBean.selectById(staff.getOrganizationId());
        this.authenticatedId = staff.getId();
        this.role = staff.getRole();
        this.fullName = staff.getFullName();
        this.pendingStaffId = null;
        this.otpSent = false;
        this.otpInput = null;

        logger.debugf("Completed sign-in for staff %s (%s) in organization %s",
                staff.getId(), staff.getRole(), staff.getOrganizationId());
        return "/admin/dashboard.xhtml?faces-redirect=true";
    }

    public String signOut() {
        logger.tracef("Signing out staff %s", authenticatedId);
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
     * deactivated -- in that case the bean self-clears its authenticated
     * state so the caller (typically {@code AdminAuthFilter}) can
     * bounce the request to login.
     */
    public boolean refreshFromDb() {
        logger.tracef("Refreshing cached session state from DB for staff %s", authenticatedId);
        if (authenticatedId == null) {
            return false;
        }

        final var match = authService.findById(authenticatedId);
        if (match.isEmpty() || !match.get().isActive()) {
            logger.debugf("Cleared session for staff %s -- account is missing or inactive", authenticatedId);
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
        return role == Role.ADMIN;
    }

    public Role getRole() {
        return role;
    }

    public UUID getAuthenticatedId() {
        return authenticatedId;
    }

    public String getFullName() {
        return fullName;
    }

    // TODO (M2): externalize via i18n.get("staff.role.admin" / "staff.role.staff")
    public String getRoleLabel() {
        if (role == null) {
            return "";
        }

        return role == Role.ADMIN ? "Admin" : "Staff";
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
