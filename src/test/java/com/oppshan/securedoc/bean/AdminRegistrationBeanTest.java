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

import java.util.ArrayList;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminRegistrationBeanTest {

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

        bean = new AdminRegistrationBean(authService, system, i18n, logger);

        capturedMessages = new ArrayList<>();
        willAnswer(invocation -> {
            capturedMessages.add(invocation.getArgument(1));
            return null;
        }).given(facesContext).addMessage(any(), any(FacesMessage.class));

        given(facesContext.getExternalContext()).willReturn(externalContext);
        given(externalContext.getFlash()).willReturn(flash);

        facesContextMock = mockStatic(FacesContext.class);
        facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
    }

    @AfterEach
    void tearDown() {
        facesContextMock.close();
    }

    @Test
    void shouldRejectRegisterWhenAnyFieldMissing() {
        // The bean no longer carries an explicit "all fields required" check --
        // required-field validation now lives on StaffRegistrationCreate and is
        // surfaced via ConstraintViolationException when the service runs.
        // Drive that path: omit lastName, ensure org is selected so we get past
        // the org-null gate, and assert the violation message is surfaced.
        bean.setFirstName("Alice");
        // lastName intentionally omitted
        bean.setEmail("alice@example.test");
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter2hunter2");
        bean.setSelectedOrganization(new OrganizationView()
                .setId(UUID.randomUUID())
                .setName("Barangay 1"));

        given(violation.getMessage()).willReturn("must not be empty");
        willThrow(new jakarta.validation.ConstraintViolationException(java.util.Set.of(violation)))
                .given(authService).createStaff(any(StaffRegistrationCreate.class));

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("must not be empty"));
    }

    @Test
    void shouldRejectRegisterWhenOrganizationNotSelected() {
        primeAllFieldsValid();
        bean.setSelectedOrganization(null);

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("register.select.organization"));
    }

    @Test
    void shouldRejectRegisterWhenPasswordsMismatch() {
        primeAllFieldsValid();
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter3hunter3");

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("register.passwords.do.not.match"));
    }

    @Test
    void shouldRejectRegisterWhenPasswordTooShort() {
        // The bean no longer applies a length check directly -- @Size on
        // StaffRegistrationCreate.password fires inside createStaff and
        // surfaces as a ConstraintViolationException, which the bean catches
        // and adds the violation message to FacesMessages.
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
    void shouldRejectRegisterWhenEmailAlreadyTakenInOrganization() {
        primeAllFieldsValid();
        final var orgId = bean.getSelectedOrganization().getId();
        given(authService.emailTakenInOrganization(bean.getEmail(), orgId)).willReturn(true);

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("register.email.taken"));
    }

    @Test
    void shouldNavigateToLoginAfterSuccessfulRegister() {
        primeAllFieldsValid();
        given(authService.emailTakenInOrganization(anyString(), any(UUID.class))).willReturn(false);

        final var outcome = bean.register();

        assertThat(outcome, is("/admin/login.xhtml?faces-redirect=true"));
        final var captor = ArgumentCaptor.forClass(StaffRegistrationCreate.class);
        then(authService).should().createStaff(captor.capture());
        final var form = captor.getValue();
        assertThat(form.getFirstName(), is("Alice"));
        assertThat(form.getEmail(), is("alice@example.test"));
        assertThat(form.getOrganizationId(), is(bean.getSelectedOrganization().getId()));
    }

    @Test
    void shouldSurfaceBusinessExceptionMessageWhenCreateStaffFails() {
        primeAllFieldsValid();
        given(authService.emailTakenInOrganization(anyString(), any(UUID.class))).willReturn(false);
        willThrow(BusinessException.unknownOrganization(UUID.randomUUID()))
                .given(authService).createStaff(any(StaffRegistrationCreate.class));

        final var outcome = bean.register();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.unknown.organization"));
    }

    private void primeAllFieldsValid() {
        final var organizationView = new OrganizationView()
                .setId(UUID.randomUUID())
                .setName("Barangay 1");
        bean.setFirstName("Alice");
        bean.setLastName("Anderson");
        bean.setEmail("alice@example.test");
        bean.setPassword("hunter2hunter2");
        bean.setConfirmPassword("hunter2hunter2");
        bean.setSelectedOrganization(organizationView);
    }
}
