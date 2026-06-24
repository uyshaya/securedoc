package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.AdminAuthBean;
import com.oppshan.securedoc.bean.OrganizationBean;
import com.oppshan.securedoc.bean.SystemConfigBean;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.model.Organization;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAuthFilterTest {

    private static final String TENANT_SLUG = "apas";
    private static final String OTHER_SLUG = "santa-cruz";
    private static final String CONTEXT_PATH = "";

    @Mock
    private AdminAuthBean adminAuthBean;

    @Mock
    private OrganizationBean organizationBean;

    @Mock
    private SystemConfigBean systemConfig;

    @Mock
    private Logger logger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    private AdminAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminAuthFilter(adminAuthBean, organizationBean, systemConfig, logger);
        given(request.getContextPath()).willReturn(CONTEXT_PATH);
        given(systemConfig.findOrganizationByCode(TENANT_SLUG))
                .willReturn(Optional.of(tenantView(TENANT_SLUG)));
        given(systemConfig.findOrganizationByCode(OTHER_SLUG))
                .willReturn(Optional.of(tenantView(OTHER_SLUG)));
    }

    // -- extractSlug (white-box) ----------------------------------

    @Test
    void shouldExtractSlugFromAdminPathWithTail() {
        assertThat(AdminAuthFilter.extractSlug("/admin/apas/dashboard.xhtml"), is("apas"));
    }

    @Test
    void shouldExtractSlugFromAdminPathWithoutTail() {
        assertThat(AdminAuthFilter.extractSlug("/admin/apas"), is("apas"));
    }

    @Test
    void shouldRejectSlugCandidateContainingDot() {
        // /admin/login.xhtml (old-style URL) must NOT be treated as slug
        // "login.xhtml" -- it falls through to query-form / 404 instead.
        assertThat(AdminAuthFilter.extractSlug("/admin/login.xhtml"), is(nullValue()));
    }

    @Test
    void shouldRejectPathOutsideAdminPrefix() {
        assertThat(AdminAuthFilter.extractSlug("/request/apas"), is(nullValue()));
    }

    @Test
    void shouldRejectEmptySlug() {
        assertThat(AdminAuthFilter.extractSlug("/admin/"), is(nullValue()));
    }

    @Test
    void shouldRejectReservedAdminSubdirectoryNamesAsSlugs() {
        // /admin/residents/X.xhtml and /admin/staff/X.xhtml are real on-disk
        // paths; extractSlug must not mistake the subdir name for a tenant
        // slug or postbacks against those pages 404 to not-found.
        assertThat(AdminAuthFilter.extractSlug("/admin/residents/residents-management.xhtml"),
                is(nullValue()));
        assertThat(AdminAuthFilter.extractSlug("/admin/staff/staff-management.xhtml"),
                is(nullValue()));
    }

    @Test
    void shouldFallBackToSessionSlugWhenPostbackHitsReservedSubdirPath() throws IOException, ServletException {
        // POST emitted by JSF for a form on residents-management.xhtml goes to
        // /admin/residents/residents-management.xhtml. The first segment is
        // a subdir name, not a slug -- the session-pinned tenant resolves it.
        given(request.getServletPath()).willReturn("/admin/residents/residents-management.xhtml");
        given(request.getParameter("slug")).willReturn(null);
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(adminAuthBean.isAdmin()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // -- doFilter: structural carve-outs --------------------------

    @Test
    void shouldBounceBareAdminToHomepage() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/");
        then(chain).should(never()).doFilter(request, response);
    }

    @Test
    void shouldBounceBareAdminSlashToHomepage() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/");
    }

    @Test
    void shouldForwardOldStyleAdminUrlToNotFound() throws IOException, ServletException {
        // /admin/login.xhtml direct, no slug query: classic stale bookmark.
        given(request.getServletPath()).willReturn("/admin/login.xhtml");
        given(request.getParameter("slug")).willReturn(null);
        given(request.getRequestDispatcher("/admin/not-found.xhtml")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldForwardUnknownSlugToNotFound() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/no-such-org/login.xhtml");
        given(systemConfig.findOrganizationByCode("no-such-org")).willReturn(Optional.empty());
        given(request.getRequestDispatcher("/admin/not-found.xhtml")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldPassThroughNotFoundPageDirectly() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/not-found.xhtml");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRedirectBareSlugUrlToTenantLogin() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/admin/apas/login.xhtml");
    }

    // -- slug-in-path form (public-facing URL) --------------------

    @Test
    void shouldForwardUnauthenticatedTenantLoginToOnDiskFile() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/login.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(false);
        given(request.getRequestDispatcher("/admin/login.xhtml?slug=apas")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
        then(chain).should(never()).doFilter(request, response);
    }

    @Test
    void shouldForwardUnauthenticatedTenantRegisterToOnDiskFile() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/register.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(false);
        given(request.getRequestDispatcher("/admin/register.xhtml?slug=apas")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldForwardAuthenticatedTenantDashboardToOnDiskFile() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/dashboard.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);
        given(request.getRequestDispatcher("/admin/dashboard.xhtml?slug=apas")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldPreserveOriginalQueryWhenForwardingSlugPath() throws IOException, ServletException {
        // /admin/apas/dashboard.xhtml?wrongTenant=1 -> /admin/dashboard.xhtml?slug=apas&wrongTenant=1
        given(request.getServletPath()).willReturn("/admin/apas/dashboard.xhtml");
        given(request.getQueryString()).willReturn("wrongTenant=1");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);
        given(request.getRequestDispatcher("/admin/dashboard.xhtml?slug=apas&wrongTenant=1"))
                .willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldRedirectUnauthenticatedSlugPathToTenantLogin() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/dashboard.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/admin/apas/login.xhtml?expired=1");
        then(chain).should(never()).doFilter(request, response);
    }

    @Test
    void shouldInvalidateSessionAndBounceWhenRefreshFromDbFails() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/dashboard.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(false);
        given(request.getSession(false)).willReturn(session);

        filter.doFilter(request, response, chain);

        verify(session).invalidate();
        verify(response).sendRedirect("/admin/apas/login.xhtml?inactive=1");
    }

    @Test
    void shouldSoftBounceCrossTenantSlugPathToOwnDashboard() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/santa-cruz/dashboard.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/admin/apas/dashboard.xhtml?wrongTenant=1");
        then(chain).should(never()).doFilter(request, response);
    }

    @Test
    void shouldSoftBounceAuthenticatedRequestToCrossTenantLogin() throws IOException, ServletException {
        // Already-authenticated visit to another tenant's login.xhtml gets
        // bounced like any other cross-tenant URL.
        given(request.getServletPath()).willReturn("/admin/santa-cruz/login.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/admin/apas/dashboard.xhtml?wrongTenant=1");
    }

    @Test
    void shouldDenyAdminOnlyPathToNonAdminStaff() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/staff/staff-management.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);
        given(adminAuthBean.isAdmin()).willReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/admin/apas/dashboard.xhtml?denied=1");
    }

    @Test
    void shouldAllowAdminOnlyPathToAdmin() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/apas/staff/staff-management.xhtml");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);
        given(adminAuthBean.isAdmin()).willReturn(true);
        given(request.getRequestDispatcher("/admin/staff/staff-management.xhtml?slug=apas"))
                .willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
    }

    // -- slug-in-query form (postback / forward target) ----------

    @Test
    void shouldPassThroughPostbackToOnDiskFileWithSlugQuery() throws IOException, ServletException {
        // JSF AJAX POST emits the form action as /admin/login.xhtml?slug=apas;
        // filter must allow it through (unauthenticated is fine -- it's the
        // login submission).
        given(request.getServletPath()).willReturn("/admin/login.xhtml");
        given(request.getParameter("slug")).willReturn(TENANT_SLUG);
        given(adminAuthBean.isAuthenticated()).willReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassThroughAuthenticatedPostbackOnDashboardWithSlugQuery() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/dashboard.xhtml");
        given(request.getParameter("slug")).willReturn(TENANT_SLUG);
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldFallBackToSessionSlugForAuthenticatedPostbackWithoutSlug() throws IOException, ServletException {
        // Non-AJAX form POST whose action URL JSF emitted without ?slug=.
        // The session-scoped org bean pins the user's tenant; fall back to
        // it so the postback resolves instead of 404ing to not-found.
        given(request.getServletPath()).willReturn("/admin/dashboard.xhtml");
        given(request.getParameter("slug")).willReturn(null);
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);
        given(organizationBean.getActiveCode()).willReturn(TENANT_SLUG);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldStillForwardToNotFoundForUnauthenticatedRequestWithoutSlug() throws IOException, ServletException {
        // Same shape as the test above but the user isn't signed in -- there's
        // no session bean to fall back to, so old-style URLs still 404.
        given(request.getServletPath()).willReturn("/admin/dashboard.xhtml");
        given(request.getParameter("slug")).willReturn(null);
        given(adminAuthBean.isAuthenticated()).willReturn(false);
        given(request.getRequestDispatcher("/admin/not-found.xhtml")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldForwardUnknownSlugQueryToNotFound() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/dashboard.xhtml");
        given(request.getParameter("slug")).willReturn("no-such-org");
        given(systemConfig.findOrganizationByCode("no-such-org")).willReturn(Optional.empty());
        given(request.getRequestDispatcher("/admin/not-found.xhtml")).willReturn(dispatcher);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher).forward(request, response);
    }

    // -- unscoped servlets ---------------------------------------

    @Test
    void shouldPassThroughUnscopedServletWhenAuthenticated() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/templates/preview");
        given(adminAuthBean.isAuthenticated()).willReturn(true);
        given(adminAuthBean.refreshFromDb()).willReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        // Servlet carve-out skips slug-vs-session matching.
        then(organizationBean).should(never()).getActiveCode();
    }

    @Test
    void shouldRedirectUnauthenticatedUnscopedServletToHomepage() throws IOException, ServletException {
        given(request.getServletPath()).willReturn("/admin/requests/document");
        given(adminAuthBean.isAuthenticated()).willReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/");
    }

    // -- helpers --------------------------------------------------

    private static OrganizationView tenantView(String code) {
        return new OrganizationView()
                .setId(UUID.randomUUID())
                .setType(Organization.Type.BARANGAY)
                .setCode(code)
                .setName("Barangay " + code);
    }
}
