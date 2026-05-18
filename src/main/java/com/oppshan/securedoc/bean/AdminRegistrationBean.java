package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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

    private static final int MIN_PASSWORD_LENGTH = 8;

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
        final var facesContext = FacesContext.getCurrentInstance();

        if (isBlank(firstName) || isBlank(lastName) || isBlank(email)
                || isBlank(password) || isBlank(confirmPassword)) {
            facesContext.addMessage(null, error(i18n.get("register.all.fields.required")));
            return null;
        }

        final var orgLabelLower = system.getOrgLabelLower();
        final UUID organizationId = selectedOrganization != null ? selectedOrganization.getId() : null;
        logger.infof("Organization ID: %s", organizationId);

        if (organizationId == null) {
            facesContext.addMessage(null, error(i18n.get("register.select.organization", orgLabelLower)));
            return null;
        }

        if (!password.equals(confirmPassword)) {
            facesContext.addMessage(null, error(i18n.get("register.passwords.do.not.match")));
            return null;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            facesContext.addMessage(null,
                    error(i18n.get("register.password.too.short", MIN_PASSWORD_LENGTH)));
            return null;
        }

        if (authService.emailTakenInOrganization(email, organizationId)) {
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
        } catch (RuntimeException exception) {
            facesContext.addMessage(null,
                    error(i18n.get("register.create.failed", exception.getMessage())));
            return null;
        }

        facesContext.getExternalContext().getFlash().setKeepMessages(true);
        facesContext.addMessage(null,
                info(i18n.get("register.submitted.for.approval", orgLabelLower)));
        return "/admin/login.xhtml?faces-redirect=true";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
}
