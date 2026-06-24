package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.AdminAuthBean;
import com.oppshan.securedoc.bean.OrganizationBean;
import com.oppshan.securedoc.bean.SystemConfigBean;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Gate for {@code /admin/*}. Tenant scope is encoded in the URL as
 * {@code /admin/{slug}/X.xhtml}; the actual on-disk file is
 * {@code /admin/X.xhtml}, so the filter forwards slug URLs onto the file
 * path with the slug carried as a {@code ?slug=} query parameter that the
 * page's {@code <f:viewParam name="slug">} picks up. Postbacks emitted by
 * JSF land at {@code /admin/X.xhtml?slug=Y} and are handled in place
 * (same auth pipeline, no forward).
 * <p>
 * Steps in order:
 * <ol>
 *   <li>Carve-out for the two non-XHTML admin servlets that aren't
 *       slug-scoped (template preview, request document download). Auth +
 *       liveness still apply; org isolation is enforced by the service-layer
 *       org-id scoping the servlets already do.</li>
 *   <li>Carve-out for {@code /admin/not-found.xhtml} so it can be the
 *       forward target without recursion.</li>
 *   <li>Bare {@code /admin} or {@code /admin/} 302s to the homepage.</li>
 *   <li>Slug-in-path form {@code /admin/{slug}/X.xhtml}: validate slug,
 *       run auth pipeline against it, then forward to
 *       {@code /admin/X.xhtml?slug={slug}}.</li>
 *   <li>Slug-in-query form {@code /admin/X.xhtml?slug={slug}} (postback or
 *       internal forward target): validate slug, run auth pipeline,
 *       pass through.</li>
 *   <li>{@code /admin/X.xhtml} without {@code slug} query: 404 forward to
 *       {@code /admin/not-found.xhtml} (old-style unscoped URL).</li>
 * </ol>
 */
@WebFilter(urlPatterns = "/admin/*")
public class AdminAuthFilter implements Filter {

    private static final String ADMIN_PREFIX = "/admin/";
    private static final String NOT_FOUND_PAGE = "/admin/not-found.xhtml";
    private static final String SLUG_QUERY_PARAM = "slug";

    private static final Set<String> UNSCOPED_ADMIN_SERVLET_PATHS = Set.of(
            "/admin/templates/preview",
            "/admin/requests/document"
    );

    private static final Set<String> PUBLIC_TAILS = Set.of(
            "/login.xhtml",
            "/register.xhtml"
    );

    private static final Set<String> ADMIN_ONLY_TAIL_PREFIXES = Set.of(
            "/staff/",
            "/residents/"
    );

    // Subdirectory names under META-INF/resources/admin/. The filter must
    // never confuse these with tenant slugs, because postback URLs JSF emits
    // for forms on those pages (e.g., /admin/staff/staff-management.xhtml)
    // start with the subdir name and would otherwise resolve as a bad slug.
    private static final Set<String> RESERVED_ADMIN_SUBDIRS = Set.of(
            "residents",
            "staff"
    );

    private final AdminAuthBean adminAuthBean;
    private final OrganizationBean organizationBean;
    private final SystemConfigBean systemConfig;
    private final Logger logger;

    @Inject
    public AdminAuthFilter(AdminAuthBean adminAuthBean,
                           OrganizationBean organizationBean,
                           SystemConfigBean systemConfig,
                           Logger logger) {
        this.adminAuthBean = adminAuthBean;
        this.organizationBean = organizationBean;
        this.systemConfig = systemConfig;
        this.logger = logger;
    }

    protected AdminAuthFilter() {
        this(null, null, null, null);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        final var request = (HttpServletRequest) servletRequest;
        final var response = (HttpServletResponse) servletResponse;
        final var path = request.getServletPath();
        logger.tracef("Filtering admin request for %s (authenticated: %s)",
                path, adminAuthBean.isAuthenticated());

        if (UNSCOPED_ADMIN_SERVLET_PATHS.contains(path)) {
            handleUnscopedServlet(request, response, chain, path);
            return;
        }

        if (NOT_FOUND_PAGE.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        if ("/admin".equals(path) || "/admin/".equals(path)) {
            logger.debugf("Bouncing bare admin path %s to homepage", path);
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        final var pathSlug = extractSlug(path);
        if (pathSlug != null) {
            handleSlugInPathRequest(request, response, pathSlug, path);
            return;
        }

        final var querySlug = request.getParameter(SLUG_QUERY_PARAM);
        if (querySlug != null && !querySlug.isBlank()) {
            handleSlugInQueryRequest(request, response, chain, querySlug, path);
            return;
        }

        // Authenticated postbacks may land here when JSF's form action URL
        // drops the slug query string (non-AJAX form submits in particular).
        // The user's tenant is already pinned in the session-scoped bean, so
        // fall back to it; the slug-vs-session check in the auth pipeline
        // would have caught a wrong tenant anyway.
        final var sessionSlug = adminAuthBean.isAuthenticated()
                ? organizationBean.getActiveCode()
                : null;
        if (sessionSlug != null && !sessionSlug.isBlank()) {
            handleSlugInQueryRequest(request, response, chain, sessionSlug, path);
            return;
        }

        logger.debugf("Path %s has no slug -- forwarding to not-found", path);
        forwardToNotFound(request, response);
    }

    /**
     * Handles the public-facing URL shape {@code /admin/{slug}/X.xhtml}.
     * Validates the slug, runs the auth pipeline, then forwards to
     * {@code /admin/X.xhtml?slug={slug}} so JSF can serve the on-disk file.
     */
    private void handleSlugInPathRequest(HttpServletRequest request,
                                         HttpServletResponse response,
                                         String slug,
                                         String path) throws IOException, ServletException {
        final var organization = systemConfig.findOrganizationByCode(slug);
        if (organization.isEmpty()) {
            logger.debugf("Slug '%s' did not resolve to an active organization -- forwarding to not-found", slug);
            forwardToNotFound(request, response);
            return;
        }

        final var tail = path.substring(ADMIN_PREFIX.length() + slug.length());
        if (tail.isEmpty() || "/".equals(tail)) {
            // /admin/{slug} or /admin/{slug}/ -- no concrete page picked.
            // Send the visitor to the per-tenant login.
            response.sendRedirect(request.getContextPath()
                    + "/admin/" + urlEncode(slug) + "/login.xhtml");
            return;
        }

        if (!runAuthPipeline(request, response, slug, tail)) {
            return;
        }

        final var forwardTarget = ADMIN_PREFIX + tail.substring(1)
                + "?" + SLUG_QUERY_PARAM + "=" + urlEncode(slug)
                + appendOriginalQuery(request);
        request.getRequestDispatcher(forwardTarget).forward(request, response);
    }

    /**
     * Handles the postback / forwarded URL shape {@code /admin/X.xhtml?slug=Y}.
     * Runs the same auth pipeline as the path-form path but passes the
     * request through (JSF is already at the on-disk file).
     */
    private void handleSlugInQueryRequest(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          String slug,
                                          String path) throws IOException, ServletException {
        final var organization = systemConfig.findOrganizationByCode(slug);
        if (organization.isEmpty()) {
            logger.debugf("Query slug '%s' did not resolve to an active organization -- forwarding to not-found", slug);
            forwardToNotFound(request, response);
            return;
        }

        final var tail = path.substring(ADMIN_PREFIX.length() - 1);
        if (!runAuthPipeline(request, response, slug, tail)) {
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Common auth pipeline for both URL shapes. Returns true when the
     * request should proceed, false when the filter has already written
     * a redirect or 404 forward (caller must abort).
     */
    private boolean runAuthPipeline(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String slug,
                                    String tail) throws IOException, ServletException {
        final var isPublicTail = PUBLIC_TAILS.contains(tail);

        if (isPublicTail && !adminAuthBean.isAuthenticated()) {
            return true;
        }

        if (!adminAuthBean.isAuthenticated()) {
            logger.debugf("Redirecting unauthenticated request for /admin/%s%s to tenant login", slug, tail);
            response.sendRedirect(request.getContextPath()
                    + "/admin/" + urlEncode(slug) + "/login.xhtml?expired=1");
            return false;
        }

        if (!adminAuthBean.refreshFromDb()) {
            logger.debugf("Bouncing request for /admin/%s%s -- session staff is missing or inactive", slug, tail);
            final var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(request.getContextPath()
                    + "/admin/" + urlEncode(slug) + "/login.xhtml?inactive=1");
            return false;
        }

        final var sessionSlug = organizationBean.getActiveCode();
        if (sessionSlug != null && !sessionSlug.equals(slug)) {
            logger.debugf("Cross-tenant access by staff in %s to slug %s -- bouncing to own dashboard",
                    sessionSlug, slug);
            response.sendRedirect(request.getContextPath()
                    + "/admin/" + urlEncode(sessionSlug) + "/dashboard.xhtml?wrongTenant=1");
            return false;
        }

        if (isAdminOnly(tail) && !adminAuthBean.isAdmin()) {
            logger.debugf("Denied admin-only path /admin/%s%s to non-admin staff %s",
                    slug, tail, adminAuthBean.getAuthenticatedId());
            response.sendRedirect(request.getContextPath()
                    + "/admin/" + urlEncode(slug) + "/dashboard.xhtml?denied=1");
            return false;
        }

        return true;
    }

    private void handleUnscopedServlet(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain chain,
                                       String path) throws IOException, ServletException {
        if (!adminAuthBean.isAuthenticated()) {
            logger.debugf("Redirecting unauthenticated servlet request for %s to homepage", path);
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        if (!adminAuthBean.refreshFromDb()) {
            logger.debugf("Bouncing servlet request for %s -- session staff is missing or inactive", path);
            final var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Returns the first path segment after {@code /admin/}, or {@code null}
     * when the path is not under {@code /admin/}, the segment is empty,
     * contains a {@code .} character (so {@code /admin/login.xhtml} does
     * not impersonate a slug), or matches a reserved admin subdirectory
     * name (so postback URLs JSF emits for forms inside those subdirs
     * fall through to the query / session-slug branch instead of being
     * misidentified as a bad tenant slug).
     */
    static String extractSlug(String path) {
        if (path == null || !path.startsWith(ADMIN_PREFIX)) {
            return null;
        }

        final var remainder = path.substring(ADMIN_PREFIX.length());
        final var firstSlash = remainder.indexOf('/');
        final var candidate = firstSlash < 0 ? remainder : remainder.substring(0, firstSlash);
        if (candidate.isBlank()
                || candidate.indexOf('.') >= 0
                || RESERVED_ADMIN_SUBDIRS.contains(candidate)) {
            return null;
        }

        return candidate;
    }

    private static boolean isAdminOnly(String tail) {
        for (final var prefix : ADMIN_ONLY_TAIL_PREFIXES) {
            if (tail.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static String appendOriginalQuery(HttpServletRequest request) {
        final var query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return "";
        }

        return "&" + query;
    }

    private static void forwardToNotFound(HttpServletRequest request,
                                          HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.getRequestDispatcher(NOT_FOUND_PAGE).forward(request, response);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
