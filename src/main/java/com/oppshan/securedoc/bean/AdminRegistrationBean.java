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

import java.util.List;
import java.util.UUID;

/**
 * Backs /admin/register.xhtml. Self-service staff registration:
 * collects basic fields + the applicant's organization, validates, hashes
 * the password (via {@link AdminAuthService} -> PasswordService), and
 * persists the Staff row with {@code active = false}.
 *
 * <p>The organization picker is a {@code <p:autoComplete>} backed by
 * {@link #completeOrganization(String)}; the bean holds the full
 * {@link OrganizationView} so JSF can re-render the chosen label after
 * validation failures.
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

    private String firstName;
    private String lastName;
    private String email;
    private OrganizationView selectedOrganization;
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

    /** Called by {@code <p:autoComplete completeMethod>} as the user types. */
    public List<OrganizationView> completeOrganization(String query) {
        return system.searchOrganizations(query);
    }

    public String register() {
        logger.tracef("Submitting staff registration for %s in organization %s",
                email, selectedOrganization == null ? null : selectedOrganization.getId());
        final var facesContext = FacesContext.getCurrentInstance();
        final var orgLabelLower = system.getOrgLabelLower();
        final UUID organizationId = selectedOrganization != null ? selectedOrganization.getId() : null;

        // Cross-field checks the DTO can't express on its own. Required-field
        // and email-format / password-length checks live on StaffRegistrationCreate
        // and fire when authService.createStaff(form) runs.
        if (organizationId == null) {
            logger.debugf("Rejected registration for %s -- no organization selected", email);
            facesContext.addMessage(null, error(i18n.get("register.select.organization", orgLabelLower)));
            return null;
        }

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
        return "/admin/login.xhtml?faces-redirect=true";
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

    public OrganizationView getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(OrganizationView selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
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
