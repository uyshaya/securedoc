package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.OrganizationBean;
import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.service.TemplateManagementService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.Serial;
import java.util.UUID;

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

    private static final String DEFAULT_MIME_TYPE = "application/pdf";

    private final TemplateManagementService templateManagementService;

    private final OrganizationBean organizationBean;

    private final I18n i18n;

    private final Logger logger;

    @Inject
    public TemplatePreviewServlet(TemplateManagementService templateManagementService,
                                  OrganizationBean organizationBean,
                                  I18n i18n,
                                  Logger logger) {
        this.templateManagementService = templateManagementService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected TemplatePreviewServlet() {
        this(null, null, null, null);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        final var idParam = request.getParameter("id");
        logger.tracef("Serving template preview for id %s", idParam);
        if (idParam == null || idParam.isBlank()) {
            logger.debugf("Rejected template preview -- id parameter is missing");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, i18n.get("template.preview.id.missing"));
            return;
        }

        final UUID templateId;
        try {
            templateId = UUID.fromString(idParam.trim());
        } catch (IllegalArgumentException invalidUuid) {
            logger.debugf("Rejected template preview -- id '%s' is not a valid UUID", idParam);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, i18n.get("template.preview.id.invalid"));
            return;
        }

        final var organizationId = organizationBean.getActiveId();
        final var content = templateManagementService.loadForPreview(organizationId, templateId);
        if (content == null) {
            logger.debugf("Template preview not found -- template %s does not belong to organization %s",
                    templateId, organizationId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        logger.debugf("Streaming %d bytes of template %s preview to organization %s",
                content.data().length, templateId, organizationId);
        response.setContentType(content.mimeType() == null ? DEFAULT_MIME_TYPE : content.mimeType());
        response.setContentLength(content.data().length);
        final var safeFileName = content.fileName() == null
                ? "template.pdf"
                : content.fileName().replace("\"", "");
        response.setHeader("Content-Disposition", "inline; filename=\"" + safeFileName + ".pdf\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(content.data());
        response.getOutputStream().flush();
    }
}
