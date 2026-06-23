package com.oppshan.securedoc.web;

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

/**
 * Routes {@code /request/*} so the resident-portal wizard is reachable only
 * through an org-scoped slug. {@code GET /request/{slug}} (with or without a
 * trailing slash), where the slug resolves to an active org of the active
 * type, is forwarded to {@code /request/portal.xhtml?orgCode={slug}};
 * everything else 404s to {@code /request/not-found.xhtml}. POSTs to
 * {@code /request/portal.xhtml} pass through unchanged because JSF emits the
 * form action with {@code orgCode} round-tripped from {@code <f:viewParam>},
 * so wizard postbacks always arrive carrying the slug as a query parameter.
 */
@WebFilter(urlPatterns = "/request/*")
public class RequestPortalFilter implements Filter {

    private static final String REQUEST_PREFIX = "/request/";
    private static final String PORTAL_PAGE = "/request/portal.xhtml";
    private static final String NOT_FOUND_PAGE = "/request/not-found.xhtml";
    private static final String ORG_CODE_PARAM = "orgCode";
    private static final String METHOD_POST = "POST";

    private final SystemConfigBean systemConfig;
    private final Logger logger;

    @Inject
    public RequestPortalFilter(SystemConfigBean systemConfig, Logger logger) {
        this.systemConfig = systemConfig;
        this.logger = logger;
    }

    protected RequestPortalFilter() {
        this(null, null);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        final var request = (HttpServletRequest) servletRequest;
        final var response = (HttpServletResponse) servletResponse;
        final var path = request.getServletPath();
        logger.tracef("Filtering request-portal request for %s", path);

        // Match the internal pages by exact path before extracting a slug, so a
        // hypothetical org with code "portal" or "not-found" can't shadow them.
        if (NOT_FOUND_PAGE.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (PORTAL_PAGE.equals(path)) {
            // POSTs are JSF postbacks against the already-pinned session bean
            // and may arrive without an orgCode in the query string; GETs must
            // carry one (set by the slug forward below).
            if (METHOD_POST.equalsIgnoreCase(request.getMethod()) || hasOrgCodeParam(request)) {
                chain.doFilter(request, response);
                return;
            }
            logger.debugf("Blocked unscoped GET %s -- forwarding to not-found", path);
            forwardToNotFound(request, response);
            return;
        }

        final var slug = extractSlug(path);
        if (slug == null) {
            logger.debugf("Path %s does not match /request/{slug} -- forwarding to not-found", path);
            forwardToNotFound(request, response);
            return;
        }

        final var organization = systemConfig.findOrganizationByCode(slug);
        if (organization.isEmpty()) {
            logger.debugf("Slug '%s' did not resolve to an active organization -- forwarding to not-found", slug);
            forwardToNotFound(request, response);
            return;
        }

        final var forwardTarget = PORTAL_PAGE + "?" + ORG_CODE_PARAM + "=" + urlEncode(slug);
        request.getRequestDispatcher(forwardTarget).forward(request, response);
    }

    private static String extractSlug(String path) {
        if (!path.startsWith(REQUEST_PREFIX)) {
            return null;
        }

        var remainder = path.substring(REQUEST_PREFIX.length());
        if (remainder.endsWith("/")) {
            remainder = remainder.substring(0, remainder.length() - 1);
        }

        if (remainder.isBlank() || remainder.indexOf('/') >= 0) {
            return null;
        }

        return remainder;
    }

    private static boolean hasOrgCodeParam(HttpServletRequest request) {
        final var raw = request.getParameter(ORG_CODE_PARAM);
        return raw != null && !raw.isBlank();
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