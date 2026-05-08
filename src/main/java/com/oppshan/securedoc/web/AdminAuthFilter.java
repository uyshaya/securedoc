package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.AdminAuthBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Gate for /admin/*. If the session is gone (timeout) or the user is
 * not authenticated, redirects to /admin/login.xhtml?expired=1.
 */
@WebFilter(urlPatterns = "/admin/*")
public class AdminAuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/admin/login.xhtml",
            "/admin/register.xhtml"
    );

    @Inject
    AdminAuthBean adminAuthBean;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();
        // login / register pages are public so can pass through
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        if (!adminAuthBean.isAuthenticated()) {
            response.sendRedirect(request.getContextPath() + "/admin/login.xhtml?expired=1");
            return;
        }

        chain.doFilter(req, res);
    }
}
