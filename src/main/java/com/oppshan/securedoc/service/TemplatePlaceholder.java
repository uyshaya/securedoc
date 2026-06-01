package com.oppshan.securedoc.service;

/**
 * Vocabulary of substitution tokens supported by
 * {@link TemplateMarkerRenderer}. Each constant carries its
 * {@code {{token}}} string -- that's the literal admins type into Word
 * when authoring a template ("place {@code {{residentName}}} where the
 * resident's name should appear"). New tokens require a matching entry
 * in {@link DocumentGenerationService#buildValueMap}.
 *
 * <p>{@link #RESIDENT_ADDRESS} is reserved for the eventual
 * address-on-{@code Requester} schema change; it has no value source yet
 * and is intentionally absent from the issued-document value map.
 */
public enum TemplatePlaceholder {
    RESIDENT_NAME("residentName"),
    RESIDENT_ADDRESS("residentAddress"),
    REQUEST_REASON("requestReason"),
    DATE_ISSUED("dateIssued"),
    REFERENCE_NUMBER("referenceNumber"),
    DOCUMENT_NAME("documentName"),
    ORGANIZATION_NAME("organizationName");

    private final String token;

    TemplatePlaceholder(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }
}
