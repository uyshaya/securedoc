package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.OrganizationBean;
import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.service.DocumentService;
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
 * Streams an issued document's persisted PDF bytes inline (new browser tab).
 * Reads from the {@code document} table -- the row was written by
 * {@link DocumentService#issueForRequest} when staff approved the request --
 * so every download returns the exact same bytes that were stamped with the
 * issuance audit metadata. Tenant isolation is enforced inside the service's
 * JPQL via {@code d.request.organization.id}; {@code AdminAuthFilter} already
 * gates {@code /admin/*}.
 */
@WebServlet(name = "requestDocument", urlPatterns = "/admin/requests/document")
public class RequestDocumentServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 5519284736192847510L;

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final DocumentService documentService;

    private final OrganizationBean organizationBean;

    private final I18n i18n;

    private final Logger logger;

    @Inject
    public RequestDocumentServlet(DocumentService documentService,
                                  OrganizationBean organizationBean,
                                  I18n i18n,
                                  Logger logger) {
        this.documentService = documentService;
        this.organizationBean = organizationBean;
        this.i18n = i18n;
        this.logger = logger;
    }

    protected RequestDocumentServlet() {
        this(null, null, null, null);
    }

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse response)
            throws ServletException, IOException {

        final var idParam = httpRequest.getParameter("id");
        logger.tracef("Serving issued document for request %s", idParam);
        if (idParam == null || idParam.isBlank()) {
            logger.debugf("Rejected document request -- id parameter is missing");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, i18n.get("request.document.id.missing"));
            return;
        }

        final UUID requestId;
        try {
            requestId = UUID.fromString(idParam.trim());
        } catch (IllegalArgumentException invalidUuid) {
            logger.debugf("Rejected document request -- id '%s' is not a valid UUID", idParam);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, i18n.get("request.document.id.invalid"));
            return;
        }

        final var organizationId = organizationBean.getActiveId();
        final var download = documentService.findDownloadForRequest(requestId, organizationId).orElse(null);
        if (download == null) {
            logger.debugf("No issued document for request %s in organization %s", requestId, organizationId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final var bytes = download.getDocumentData();
        logger.debugf("Streaming %d bytes of issued document for request %s", bytes.length, requestId);
        response.setContentType(PDF_MIME_TYPE);
        response.setContentLength(bytes.length);
        response.setHeader("Content-Disposition", "inline; filename=\"" + download.getFileName() + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
