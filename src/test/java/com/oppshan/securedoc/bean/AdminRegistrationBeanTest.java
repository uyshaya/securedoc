package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.service.AdminAuthService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminRegistrationBeanTest {

    private static final String TENANT_SLUG = "apas";
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock
    private AdminAuthService authService;

    @Mock
    private SystemConfigBean system;

    @Mock
    private I18n i18n;

    @Mock
    private Logger logger;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private Flash flash;

    @Mock
    private jakarta.validation.ConstraintViolation<?> violation;

    private AdminRegistrationBean bean;
    private MockedStatic<FacesContext> facesContextMock;
    private List<FacesMessage> capturedMessages;

    @BeforeEach
    void setUp() {
        given(system.getOrgLabelLower()).willReturn("barangay");
        given(i18n.get(anyString())).willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object[].class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(system.findOrganizationByCode(TENANT_SLUG))
                .willReturn(Optional.of(tenantView()));

        bean = new AdminRegistrationBean(authService, system, i18n, logger);

        capturedMessages = new ArrayList<>();
        willAnswer(invocation -> {
            capturedMessages.add(invocation.getArgument(1));
            return null;
        }).given(facesContext).addMessage(any(), any(FacesMessage.class));

        given(facesContext.getExternalContext()).willReturn(externalContext);
        given(externalContext.getFlash()).willReturn(flash);
        given(externalContext.getRequestContextPath()).willReturn("");

        facesContextMock = mockStatic(FacesContext.class);
        facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
    }

    @AfterEach
    void tearDown() {
        facesContextMock.close();
    }

    @Test
    void shouldPinOrganizationFromUrlSlug() {
        bean.setUrlSlug(TENANT_SLUG);

        bean.initRegisterFromUrl();

        assertThat(bean.getPinnedOrganization(), is(tenantView()));
    }

    @Test
    void shouldRejectRegisterWhenTenantNotPinned() throws IOException {
        primeAllFieldsValid();
        // No view-action firing -- pinnedOrganization stays null.

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("auth.tenant.missing"));
    }

    @Test
    void shouldRejectRegisterWhenAnyFieldMissing() throws IOException {
        // The bean no longer carries an explicit "all fields required" check --
        // required-field validation now lives on StaffRegistrationCreate and is
        // surfaced via ConstraintViolationException when the service runs.
        pinTenant();
        bean.setFirstName("Alice");
        // lastName intentionally omitted
        bean.setEmail("alice@example.test");
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter2hunter2");

        given(violation.getMessage()).willReturn("must not be empty");
        willThrow(new jakarta.validation.ConstraintViolationException(java.util.Set.of(violation)))
                .given(authService).createStaff(any(StaffRegistrationCreate.class));

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("must not be empty"));
    }

    @Test
    void shouldRejectRegisterWhenPasswordsMismatch() throws IOException {
        pinTenant();
        primeAllFieldsValid();
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter3hunter3");

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("register.passwords.do.not.match"));
    }

    @Test
    void shouldRejectRegisterWhenPasswordTooShort() throws IOException {
        pinTenant();
        primeAllFieldsValid();
        bean.setPassword("short");
        bean.setConfirmPassword("short");

        given(violation.getMessage()).willReturn("size must be between 8 and 100");
        willThrow(new jakarta.validation.ConstraintViolationException(java.util.Set.of(violation)))
                .given(authService).createStaff(any(StaffRegistrationCreate.class));

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("size must be between 8 and 100"));
    }

    @Test
    void shouldRejectRegisterWhenEmailAlreadyTakenInOrganization() throws IOException {
        pinTenant();
        primeAllFieldsValid();
        given(authService.emailTakenInOrganization(bean.getEmail(), TENANT_ID)).willReturn(true);

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("register.email.taken"));
    }

    @Test
    void shouldRedirectToSlugScopedLoginAfterSuccessfulRegister() throws IOException {
        pinTenant();
        primeAllFieldsValid();
        given(authService.emailTakenInOrganization(anyString(), any(UUID.class))).willReturn(false);

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        verify(externalContext).redirect("/admin/" + TENANT_SLUG + "/login.xhtml?registered=1");
        final var captor = ArgumentCaptor.forClass(StaffRegistrationCreate.class);
        then(authService).should().createStaff(captor.capture());
        final var form = captor.getValue();
        assertThat(form.getFirstName(), is("Alice"));
        assertThat(form.getEmail(), is("alice@example.test"));
        assertThat(form.getOrganizationId(), is(TENANT_ID));
    }

    @Test
    void shouldSurfaceBusinessExceptionMessageWhenCreateStaffFails() throws IOException {
        pinTenant();
        primeAllFieldsValid();
        given(authService.emailTakenInOrganization(anyString(), any(UUID.class))).willReturn(false);
        willThrow(BusinessException.unknownOrganization(UUID.randomUUID()))
                .given(authService).createStaff(any(StaffRegistrationCreate.class));

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.unknown.organization"));
    }

    private static OrganizationView tenantView() {
        return new OrganizationView()
                .setId(TENANT_ID)
                .setCode(TENANT_SLUG)
                .setName("Barangay Apas");
    }

    private void pinTenant() {
        bean.setUrlSlug(TENANT_SLUG);
        bean.initRegisterFromUrl();
    }

    private void primeAllFieldsValid() {
        bean.setFirstName("Alice");
        bean.setLastName("Anderson");
        bean.setEmail("alice@example.test");
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter2hunter2");
    }
}
