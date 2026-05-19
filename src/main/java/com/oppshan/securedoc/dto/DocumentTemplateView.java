package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.model.DocumentTemplate;
import jakarta.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Narrow projection of a {@link DocumentTemplate} row for the resident-side
 * certificate dropdown and the admin template list. Excludes the LONGBLOB
 * {@code template_data} -- the picker only needs id/name/description/createdAt
 * to render and bind. The blob is loaded later, at issuance time, by the
 * (future) issuance service.
 */
public class DocumentTemplateView implements Serializable {

    @Serial
    private static final long serialVersionUID = 4938201736452817392L;

    private UUID id;

    private DocumentTemplate.DocType docType;

    private String name;

    @Nullable
    private String description;

    @Nullable
    private Instant createdAt;

    public DocumentTemplateView() {
    }

    public UUID getId() {
        return id;
    }

    public DocumentTemplateView setId(UUID id) {
        this.id = id;
        return this;
    }

    public DocumentTemplate.DocType getDocType() {
        return docType;
    }

    public DocumentTemplateView setDocType(DocumentTemplate.DocType docType) {
        this.docType = docType;
        return this;
    }

    public String getName() {
        return name;
    }

    public DocumentTemplateView setName(String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public DocumentTemplateView setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nullable
    public Instant getCreatedAt() {
        return createdAt;
    }

    public DocumentTemplateView setCreatedAt(@Nullable Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final DocumentTemplateView that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               docType == that.docType &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, docType, name, description, createdAt);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("docType", docType)
                .add("name", name)
                .add("description", description)
                .add("createdAt", createdAt)
                .toString();
    }
}
