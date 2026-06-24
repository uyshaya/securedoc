package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Backs /admin/{slug}/register.xhtml. Self-service staff registration:
 * collects basic fields, validates, hashes the password (via
 * {@link AdminAuthService} -> PasswordService), and persists the Staff
 * row with {@code active = false}.
 *
 * <p>The organization is pinned by the URL slug rather than picked from a
 * dropdown -- the view-action {@link #initRegisterFromUrl()} resolves
 * {@code <f:viewParam name="slug">} into {@link #pinnedOrganization}
 * before the form renders.
 *
 * <p>An admin must approve the account (flip the flag) before sign-in
 * is allowed; that's enforced in {@code AdminAuthBean.signIn}.
 */
@Named("registerBean")
@RequestScoped
public class AdminRegistrationBean {

    private final AdminAuthService authService;
    private final SystemConfigBean system;
    private final I18n i18n;
    private final Logger logger;

    private String urlSlug;
    private OrganizationView pinnedOrganization;

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String confirmPassword;

    @Inject
    public AdminRegistrationBean(AdminAuthService authService,
                                 SystemConfigBean system,
                                 I18n i18n,
                                 Logger logger) {
        this.authService = authService;
        this.system = system;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected AdminRegistrationBean() {
        this(null, null, null, null);
    }

    /**
     * Resolves the {@code <f:viewParam name="slug">} into an
     * {@link OrganizationView} and caches it for {@link #register()}.
     * The filter has already validated the slug before this fires, so a
     * blank or unresolvable slug here is defensive only.
     */
    public void initRegisterFromUrl() {
        if (urlSlug == null || urlSlug.isBlank()) {
            this.pinnedOrganization = null;
            return;
        }

        system.findOrganizationByCode(urlSlug)
                .ifPresentOrElse(
                        organization -> this.pinnedOrganization = organization,
                        () -> this.pinnedOrganization = null);
    }

    public String register() throws IOException {
        logger.tracef("Submitting staff registration for %s in slug %s", email, urlSlug);
        final var facesContext = FacesContext.getCurrentInstance();
        final var orgLabelLower = system.getOrgLabelLower();

        if (pinnedOrganization == null) {
            logger.debugf("Rejected registration for %s -- no tenant pinned from URL slug", email);
            facesContext.addMessage(null, error(i18n.get("auth.tenant.missing")));
            return null;
        }

        final var organizationId = pinnedOrganization.getId();

        // Cross-field check the DTO can't express on its own. Required-field
        // and email-format / password-length checks live on
        // StaffRegistrationCreate and fire when authService.createStaff(form)
        // runs.
        if (password != null && !password.equals(confirmPassword)) {
            logger.debugf("Rejected registration for %s -- password and confirmation do not match", email);
            facesContext.addMessage(null, error(i18n.get("register.passwords.do.not.match")));
            return null;
        }

        if (email != null && !email.isBlank()
                && authService.emailTakenInOrganization(email, organizationId)) {
            logger.debugf("Rejected registration -- email %s is already taken in organization %s",
                    email, organizationId);
            facesContext.addMessage(null, error(i18n.get("register.email.taken", orgLabelLower)));
            return null;
        }

        try {
            final var form = new StaffRegistrationCreate()
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setEmail(email)
                    .setPassword(password)
                    .setOrganizationId(organizationId);
            authService.createStaff(form);
        } catch (ConstraintViolationException violation) {
            logger.debugf("Rejected registration for %s -- %s constraint violation(s)",
                    email, violation.getConstraintViolations().size());
            violation.getConstraintViolations().forEach(constraintViolation ->
                    facesContext.addMessage(null, error(constraintViolation.getMessage())));
            return null;
        } catch (BusinessException businessException) {
            logger.debugf("Registration for %s failed with business error %s",
                    email, businessException.getMessageCode());
            facesContext.addMessage(null,
                    error(i18n.get(businessException.getMessageCode().getValue(),
                            businessException.getArguments())));
            return null;
        } catch (RuntimeException exception) {
            logger.warnf(exception, "Unexpected error while registering staff %s", email);
            facesContext.addMessage(null,
                    error(i18n.get("register.create.failed", exception.getMessage())));
            return null;
        }

        logger.debugf("Submitted registration for %s in organization %s, pending admin approval", email, organizationId);
        facesContext.getExternalContext().getFlash().setKeepMessages(true);
        facesContext.addMessage(null,
                info(i18n.get("register.submitted.for.approval", orgLabelLower)));

        // Skip JSF implicit navigation: the slug URL doesn't map to an
        // on-disk view, so the nav handler can't resolve it. Redirect
        // directly; the filter will forward the slug URL to the file.
        final var externalContext = facesContext.getExternalContext();
        externalContext.redirect(externalContext.getRequestContextPath()
                + "/admin/" + pinnedOrganization.getCode() + "/login.xhtml?registered=1");
        return null;
    }

    public String getUrlSlug() {
        return urlSlug;
    }

    public void setUrlSlug(String urlSlug) {
        this.urlSlug = urlSlug;
    }

    public OrganizationView getPinnedOrganization() {
        return pinnedOrganization;
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

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    private static FacesMessage info(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }
}
