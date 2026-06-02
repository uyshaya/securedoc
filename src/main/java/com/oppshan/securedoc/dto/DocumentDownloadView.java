package com.oppshan.securedoc.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Narrow projection for streaming an issued document from
 * {@code RequestDocumentServlet}: just the bytes and the {@code Content-
 * Disposition} filename, no audit metadata or PII. Built by
 * {@link com.oppshan.securedoc.model.Document#toDownloadView()}.
 */
public class DocumentDownloadView implements Serializable {

    @Serial
    private static final long serialVersionUID = 3958274619205843762L;

    private String fileName;
    private byte[] documentData;

    public DocumentDownloadView() {
    }

    public String getFileName() {
        return fileName;
    }

    public DocumentDownloadView setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public byte[] getDocumentData() {
        return documentData;
    }

    public DocumentDownloadView setDocumentData(byte[] documentData) {
        this.documentData = documentData;
        return this;
    }
}
