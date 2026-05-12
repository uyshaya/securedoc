package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.DocumentTemplate;

import java.io.Serial;
import java.io.Serializable;

/**
 * Narrow projection of a {@link DocumentTemplate} row for the
 * resident-side certificate dropdown. Excludes the LONGBLOB
 * {@code template_data} — the picker only needs id/name/description
 * to render and bind. The blob is loaded later, at issuance time,
 * by the (future) issuance service.
 */
public class DocumentTemplateView implements Serializable {

    @Serial
    private static final long serialVersionUID = 4938201736452817392L;

    private Long id;
    private DocumentTemplate.DocType docType;
    private String name;
    private String description;

    public DocumentTemplateView() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentTemplate.DocType getDocType() { return docType; }
    public void setDocType(DocumentTemplate.DocType docType) { this.docType = docType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
