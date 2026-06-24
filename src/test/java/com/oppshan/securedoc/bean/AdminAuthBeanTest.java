package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.model.Staff;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * Unit-style test for {@link AdminAuthBean}. Hand-instantiated with mock
 * collaborators so the test runs without booting Quarkus -- the bean's only
 * Jakarta EE touchpoint is {@code FacesContext.getCurrentInstance()}, stubbed
 * via Mockito's static mocking (which has no @Mock annotation equivalent).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAuthBeanTest {

    private static final String TENANT_SLUG = "apas";
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock
    private AdminAuthService authService;

    @Mock
    private OrganizationBean organizationBean;

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

    private AdminAuthBean adminAuthBean;
    private MockedStatic<FacesContext> facesContextMock;
    private List<FacesMessage> capturedMessages;

    @BeforeEach
    void setUp() {
        given(system.getOrgLabelLower()).willReturn("barangay");
        // Echo the key back so messages are recognizable in assertions. All three
        // I18n overloads need their own given() because Mockito matches by the
        // exact method signature, not by the runtime argument count.
        given(i18n.get(anyString())).willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object[].class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(system.findOrganizationByCode(TENANT_SLUG))
                .willReturn(Optional.of(tenantView()));

        adminAuthBean = new AdminAuthBean(authService, organizationBean, system, i18n, logger);

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
        adminAuthBean.setUrlSlug(TENANT_SLUG);

        adminAuthBean.initLoginFromUrl();

        assertThat(adminAuthBean.getPendingOrganization(), is(notNullValue()));
        assertThat(adminAuthBean.getPendingOrganization().getCode(), is(TENANT_SLUG));
    }

    @Test
    void shouldRejectSignInWhenNoTenantPinned() {
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("pw");
        // No view-action firing -- pendingOrganization stays null.

        final var outcome = adminAuthBean.signIn();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages, hasSize(1));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.tenant.missing"));
    }

    @Test
    void shouldRejectSignInWhenEmailMissing() {
        pinTenant();
        adminAuthBean.setEmail(null);
        adminAuthBean.setPassword("anything");

        final var outcome = adminAuthBean.signIn();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages, hasSize(1));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.email.or.password.required"));
    }

    @Test
    void shouldRejectSignInWhenPasswordMissing() {
        pinTenant();
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("  ");

        final var outcome = adminAuthBean.signIn();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages, hasSize(1));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.email.or.password.required"));
    }

    @Test
    void shouldRejectSignInWhenCredentialsInvalid() {
        pinTenant();
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("pw");
        given(authService.authenticate(TENANT_ID, "hello@example.test", "pw")).willReturn(Optional.empty());

        final var outcome = adminAuthBean.signIn();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.email.or.password.invalid"));
    }

    @Test
    void shouldRejectSignInWhenAccountInactive() {
        pinTenant();
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("pw");
        final var inactive = new StaffView()
                .setId(UUID.randomUUID())
                .setEmail("hello@example.test")
                .setActive(false);
        given(authService.authenticate(TENANT_ID, "hello@example.test", "pw")).willReturn(Optional.of(inactive));

        final var outcome = adminAuthBean.signIn();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.account.inactive"));
    }

    @Test
    void shouldIssueOtpAndFlipOtpSentFlagOnValidCredentials() {
        pinTenant();
        final var staffId = UUID.randomUUID();
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("pw");
        final var active = new StaffView()
                .setId(staffId)
                .setEmail("hello@example.test")
                .setActive(true)
                .setRole(Staff.Role.STAFF);
        given(authService.authenticate(TENANT_ID, "hello@example.test", "pw")).willReturn(Optional.of(active));

        adminAuthBean.signIn();

        then(authService).should().issueLoginOtp(staffId);
        assertThat(adminAuthBean.isOtpSent(), is(true));
        assertThat(adminAuthBean.getPassword(), is(nullValue()));
    }

    @Test
    void shouldVerifyOtpAndRedirectToSlugScopedDashboardOnSuccess() throws IOException {
        final var staffId = UUID.randomUUID();
        primePendingStaff(staffId);
        adminAuthBean.setOtpInput("123456");
        given(authService.verifyLoginOtp(staffId, "123456")).willReturn(true);
        final var view = new StaffView()
                .setId(staffId)
                .setEmail("hello@example.test")
                .setActive(true)
                .setRole(Staff.Role.STAFF)
                .setFullName("Hello World")
                .setOrganizationId(TENANT_ID);
        given(authService.findById(staffId)).willReturn(Optional.of(view));
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        final var outcome = adminAuthBean.verifyOtp();

        assertThat(outcome, is(nullValue()));
        verify(externalContext).redirect("/admin/" + TENANT_SLUG + "/dashboard.xhtml");
        then(authService).should().recordLogin(staffId);
        then(organizationBean).should().selectById(view.getOrganizationId());
        assertThat(adminAuthBean.getAuthenticatedId(), is(staffId));
        assertThat(adminAuthBean.isAuthenticated(), is(true));
    }

    @Test
    void shouldRejectOtpVerificationWhenServiceReturnsFalse() throws IOException {
        final var staffId = UUID.randomUUID();
        primePendingStaff(staffId);
        adminAuthBean.setOtpInput("000000");
        given(authService.verifyLoginOtp(staffId, "000000")).willReturn(false);

        final var outcome = adminAuthBean.verifyOtp();

        assertThat(outcome, is(nullValue()));
        assertThat(capturedMessages.getFirst().getSummary(), containsString("auth.otp.invalid.or.expired"));
        assertThat(adminAuthBean.isAuthenticated(), is(false));
    }

    @Test
    void shouldSignOutClearingStateAndInvalidatingSessionAndRedirectingToSlugLogin() throws IOException {
        final var staffId = UUID.randomUUID();
        primeAuthenticatedStaff(staffId, true);
        // signOut captures slug before clearing -- stub returns the pinned slug.
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        final var outcome = adminAuthBean.signOut();

        assertThat(outcome, is(nullValue()));
        verify(externalContext).redirect("/admin/" + TENANT_SLUG + "/login.xhtml");
        assertThat(adminAuthBean.isAuthenticated(), is(false));
        assertThat(adminAuthBean.getEmail(), is(nullValue()));
        then(organizationBean).should().clear();
        then(externalContext).should().invalidateSession();
    }

    @Test
    void shouldFallBackToHomepageOnSignOutWhenNoSlugInSession() throws IOException {
        final var staffId = UUID.randomUUID();
        primeAuthenticatedStaff(staffId, true);
        given(organizationBean.getActiveCode()).willReturn(null);

        final var outcome = adminAuthBean.signOut();

        assertThat(outcome, is(nullValue()));
        verify(externalContext).redirect("/");
    }

    @Test
    void shouldReturnTrueFromRefreshFromDbWhenStaffStillActive() {
        final var staffId = primeAuthenticatedStaff(UUID.randomUUID(), true);
        given(authService.findById(staffId)).willReturn(Optional.of(new StaffView()
                .setId(staffId)
                .setActive(true)
                .setRole(Staff.Role.ADMIN)
                .setFullName("Admin")));

        final var stillThere = adminAuthBean.refreshFromDb();

        assertThat(stillThere, is(true));
        assertThat(adminAuthBean.getRole(), is(Staff.Role.ADMIN));
    }

    @Test
    void shouldReturnFalseFromRefreshFromDbAndClearStateWhenStaffDeactivated() {
        final var staffId = primeAuthenticatedStaff(UUID.randomUUID(), true);
        given(authService.findById(staffId)).willReturn(Optional.of(new StaffView()
                .setId(staffId)
                .setActive(false)
                .setRole(Staff.Role.STAFF)));

        final var stillThere = adminAuthBean.refreshFromDb();

        assertThat(stillThere, is(false));
        assertThat(adminAuthBean.getAuthenticatedId(), is(nullValue()));
        assertThat(adminAuthBean.getRole(), is(nullValue()));
    }

    @Test
    void shouldExposeOrgCodeForSlugAwareLinks() {
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        assertThat(adminAuthBean.getOrgCode(), is(TENANT_SLUG));
    }

    private static OrganizationView tenantView() {
        return new OrganizationView()
                .setId(TENANT_ID)
                .setCode(TENANT_SLUG)
                .setName("Barangay Apas");
    }

    /**
     * Drives {@link AdminAuthBean#initLoginFromUrl()} so {@code pendingOrganization}
     * is populated without reaching for reflection.
     */
    private void pinTenant() {
        adminAuthBean.setUrlSlug(TENANT_SLUG);
        adminAuthBean.initLoginFromUrl();
    }

    /**
     * Drives the bean through signIn so its private {@code pendingStaffId}
     * is set without reaching for reflection.
     */
    private void primePendingStaff(UUID staffId) {
        pinTenant();
        adminAuthBean.setEmail("hello@example.test");
        adminAuthBean.setPassword("pw");
        given(authService.authenticate(TENANT_ID, "hello@example.test", "pw")).willReturn(Optional.of(
                new StaffView()
                        .setId(staffId)
                        .setActive(true)
                        .setRole(Staff.Role.STAFF)));
        adminAuthBean.signIn();
        // Reset captured messages so test assertions only see post-prime output.
        capturedMessages.clear();
    }

    /**
     * Drives the bean all the way through verifyOtp so the private
     * {@code authenticatedId} field is populated.
     */
    private UUID primeAuthenticatedStaff(UUID staffId, boolean active) {
        primePendingStaff(staffId);
        adminAuthBean.setOtpInput("123456");
        given(authService.verifyLoginOtp(staffId, "123456")).willReturn(true);
        given(authService.findById(staffId)).willReturn(Optional.of(new StaffView()
                .setId(staffId)
                .setActive(active)
                .setRole(Staff.Role.STAFF)
                .setFullName("HW")
                .setOrganizationId(TENANT_ID)));
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);
        try {
            adminAuthBean.verifyOtp();
        } catch (IOException ioException) {
            throw new AssertionError("Unexpected IO during prime", ioException);
        }
        assertThat(adminAuthBean.getAuthenticatedId(), is(notNullValue()));
        capturedMessages.clear();
        return staffId;
    }
}
