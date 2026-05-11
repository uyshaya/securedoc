package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.AdminAuthBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Gate for /admin/*. Three layers:
 *   1. Public paths (login, register) pass through unauthenticated.
 *   2. Authenticated paths require an active session — otherwise
 *      redirect to /admin/login.xhtml?expired=1.
 *   3. Admin-only paths (e.g. /admin/staff/*) additionally require
 *      role=ADMIN — otherwise redirect to /admin/dashboard.xhtml?denied=1
 *      so a signed-in staff member doesn't get logged out, just bounced.
 */
@WebFilter(urlPatterns = "/admin/*")
public class AdminAuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/admin/login.xhtml",
            "/admin/register.xhtml"
    );

    private static final List<String> ADMIN_ONLY_PREFIXES = List.of(
            "/admin/staff/"
    );

    @Inject
    AdminAuthBean adminAuthBean;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();

        // (1) login / register pages are public so users can come in.
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        // (2) Everything else needs an authenticated session.
        if (!adminAuthBean.isAuthenticated()) {
            response.sendRedirect(request.getContextPath() + "/admin/login.xhtml?expired=1");
            return;
        }

        // (2b) Per-request liveness: re-read the staff row so deactivation
        // and role changes by an admin take effect immediately rather than
        // at next login. On failure, kill the session and bounce to login.
        if (!adminAuthBean.refreshFromDb()) {
            request.getSession(false).invalidate();
            response.sendRedirect(request.getContextPath() + "/admin/login.xhtml?inactive=1");
            return;
        }

        // (3) Admin-only paths additionally require role=ADMIN.
        if (isAdminOnly(path) && !adminAuthBean.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard.xhtml?denied=1");
            return;
        }

        chain.doFilter(req, res);
    }

    private static boolean isAdminOnly(String path) {
        for (String prefix : ADMIN_ONLY_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }
}
