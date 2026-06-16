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
 * Routes {@code /user/*} so the resident-portal request page is reachable
 * only through an org-scoped slug. A {@code GET /user/{slug}/request.xhtml}
 * with a slug resolving to an active org of the active type is forwarded
 * to {@code /user/request.xhtml?orgCode={slug}}; everything else 404s to
 * {@code /user/not-found.xhtml}. POSTs to {@code /user/request.xhtml} pass
 * through unchanged because JSF emits form actions without round-tripping
 * {@code <f:viewParam>}, so wizard postbacks always arrive without the slug.
 */
@WebFilter(urlPatterns = "/user/*")
public class ResidentScopeFilter implements Filter {

    private static final String USER_PREFIX = "/user/";
    private static final String REQUEST_PAGE = "/user/request.xhtml";
    private static final String NOT_FOUND_PAGE = "/user/not-found.xhtml";
    private static final String ORG_CODE_PARAM = "orgCode";
    private static final String METHOD_POST = "POST";

    private final SystemConfigBean systemConfig;
    private final Logger logger;

    @Inject
    public ResidentScopeFilter(SystemConfigBean systemConfig, Logger logger) {
        this.systemConfig = systemConfig;
        this.logger = logger;
    }

    protected ResidentScopeFilter() {
        this(null, null);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        final var request = (HttpServletRequest) servletRequest;
        final var response = (HttpServletResponse) servletResponse;
        final var path = request.getServletPath();
        logger.tracef("Filtering resident-portal request for %s", path);

        if (NOT_FOUND_PAGE.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (REQUEST_PAGE.equals(path)) {
            // POSTs are JSF postbacks against the already-pinned session bean
            // and arrive without an orgCode; GETs must carry one (set by the
            // slug forward below).
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
            logger.debugf("Path %s does not match /user/{slug}/request.xhtml -- forwarding to not-found", path);
            forwardToNotFound(request, response);
            return;
        }

        final var organization = systemConfig.findOrganizationByCode(slug);
        if (organization.isEmpty()) {
            logger.debugf("Slug '%s' did not resolve to an active organization -- forwarding to not-found", slug);
            forwardToNotFound(request, response);
            return;
        }

        final var forwardTarget = REQUEST_PAGE + "?" + ORG_CODE_PARAM + "=" + urlEncode(slug);
        request.getRequestDispatcher(forwardTarget).forward(request, response);
    }

    private static String extractSlug(String path) {
        if (!path.startsWith(USER_PREFIX)) {
            return null;
        }

        final var afterPrefix = path.substring(USER_PREFIX.length());
        final var slashIndex = afterPrefix.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }

        final var slug = afterPrefix.substring(0, slashIndex);
        final var remainder = afterPrefix.substring(slashIndex);
        if (!"/request.xhtml".equals(remainder)) {
            return null;
        }

        return slug;
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
