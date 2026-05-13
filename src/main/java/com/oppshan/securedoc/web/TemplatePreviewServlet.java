package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.OrganizationBean;
import com.oppshan.securedoc.service.TemplateManagementService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;

/**
 * Streams a {@code document_templates.template_data} blob inline so the
 * admin's browser can render it (PDF viewer in a new tab). Tenant
 * isolation: the servlet only releases bytes for templates that belong
 * to the caller's currently-active organization, looked up from the
 * session-scoped {@link OrganizationBean}.
 *
 * <p>{@code AdminAuthFilter} already gates {@code /admin/*}, so the
 * caller is guaranteed to be a signed-in staff member.
 */
@WebServlet(name = "templatePreview", urlPatterns = "/admin/templates/preview")
public class TemplatePreviewServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 3677297042944447303L;

    @Inject
    TemplateManagementService service;

    @Inject
    OrganizationBean organizationBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter");
            return;
        }
        Long id;
        try {
            id = Long.valueOf(idParam.trim());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter");
            return;
        }

        Long orgId = organizationBean.getActiveId();
        TemplateManagementService.TemplateContent content = service.loadForPreview(orgId, id);
        if (content == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setContentType(content.mimeType() == null ? "application/pdf" : content.mimeType());
        resp.setContentLength(content.data().length);
        String safeName = content.fileName() == null ? "template.pdf" : content.fileName().replace("\"", "");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + safeName + ".pdf\"");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.getOutputStream().write(content.data());
        resp.getOutputStream().flush();
    }
}
