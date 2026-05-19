package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.dto.RequestSubmissionView;
import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.service.RequestService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestBeanTest {

    @Mock
    private SystemConfigBean system;

    @Mock
    private DocumentTemplateRepository templateRepo;

    @Mock
    private RequestService requestService;

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

    private RequestBean bean;
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

        bean = new RequestBean(system, templateRepo, requestService, i18n, logger);

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
    void shouldProceedFromLandingWhenOrganizationAndTemplateSelected() {
        bean.setSelectedOrganization(new OrganizationView().setId(UUID.randomUUID()));
        bean.setSelectedTemplateId(UUID.randomUUID());

        final var outcome = bean.proceedFromLanding();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.isEmpty(), is(true));
    }

    @Test
    void shouldRejectProceedFromLandingWhenOrganizationMissing() {
        bean.setSelectedOrganization(null);
        bean.setSelectedTemplateId(UUID.randomUUID());

        bean.proceedFromLanding();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("request.landing.organization.required"));
        then(facesContext).should().validationFailed();
    }

    @Test
    void shouldRejectProceedFromLandingWhenTemplateMissing() {
        bean.setSelectedOrganization(new OrganizationView().setId(UUID.randomUUID()));
        bean.setSelectedTemplateId(null);

        bean.proceedFromLanding();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("request.landing.certificate.required"));
        then(facesContext).should().validationFailed();
    }

    @Test
    void shouldSendOtpWhenEmailIsValid() {
        bean.setEmail("resident@example.test");

        bean.sendOtp();

        then(requestService).should().issueEmailOtp("resident@example.test");
        assertThat(bean.getEmail(), is("resident@example.test"));
        assertThat(capturedMessages.isEmpty(), is(true));
    }

    @Test
    void shouldRejectSendOtpWhenEmailInvalid() {
        bean.setEmail("not-an-email");

        bean.sendOtp();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("request.email.invalid"));
        then(facesContext).should().validationFailed();
    }

    @Test
    void shouldVerifyOtpWhenServiceReturnsTrue() {
        bean.setEmail("resident@example.test");
        bean.setOtpInput("123456");
        given(requestService.verifyEmailOtp("resident@example.test", "123456")).willReturn(true);

        bean.verifyOtp();

        assertThat(bean.isEmailVerified(), is(true));
        assertThat(bean.getOtpInput(), is(nullValue()));
    }

    @Test
    void shouldRejectVerifyOtpWhenServiceReturnsFalse() {
        bean.setEmail("resident@example.test");
        bean.setOtpInput("000000");
        given(requestService.verifyEmailOtp("resident@example.test", "000000")).willReturn(false);

        bean.verifyOtp();

        assertThat(bean.isEmailVerified(), is(false));
        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("request.otp.invalid.or.expired"));
        then(facesContext).should().validationFailed();
    }

    @Test
    void shouldSubmitRequestAndStoreSubmittedReference() {
        final var orgId = UUID.randomUUID();
        final var templateId = UUID.randomUUID();
        bean.setSelectedOrganization(new OrganizationView().setId(orgId));
        bean.setSelectedTemplateId(templateId);
        bean.setEmail("resident@example.test");
        bean.setFirstName("Jane");
        bean.setLastName("Resident");
        bean.setSex("F");
        bean.setDateOfBirth(LocalDate.of(1990, 1, 1));
        bean.setPurpose("employment");
        final var generatedReference = UUID.randomUUID().toString();
        given(requestService.submitRequest(any(RequestCreate.class)))
                .willReturn(new RequestSubmissionView()
                        .setId(UUID.randomUUID())
                        .setReferenceNumber(generatedReference));

        bean.submitRequest();

        assertThat(bean.getSubmittedReference(), is(generatedReference));
    }

    @Test
    void shouldTrackRequestSettingTrackedResultOnSuccess() {
        final var ref = UUID.randomUUID().toString();
        bean.setTrackReference(ref);
        final var view = new RequestTrackingView(ref, Request.Status.PENDING,
                "Barangay Clearance", "Barangay 1", Instant.now(), Instant.now());
        given(requestService.lookupByReference(ref)).willReturn(Optional.of(view));

        bean.trackRequest();

        assertThat(bean.getTrackedResult(), is(notNullValue()));
        assertThat(bean.getTrackedResult().getReferenceNumber(), is(ref));
        assertThat(bean.isTrackingNotFound(), is(false));
    }

    @Test
    void shouldSetTrackingNotFoundWhenLookupReturnsEmpty() {
        final var ref = UUID.randomUUID().toString();
        bean.setTrackReference(ref);
        given(requestService.lookupByReference(ref)).willReturn(Optional.empty());

        bean.trackRequest();

        assertThat(bean.getTrackedResult(), is(nullValue()));
        assertThat(bean.isTrackingNotFound(), is(true));
    }
}
