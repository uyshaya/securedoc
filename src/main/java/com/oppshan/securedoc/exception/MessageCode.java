package com.oppshan.securedoc.exception;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Symbolic codes for business-rule failures surfaced via
 * {@link BusinessException}. Each constant maps to an i18n key in
 * {@code messages.properties}; the catching layer (typically a JSF bean)
 * resolves the localized message via {@link com.oppshan.securedoc.common.I18n}
 * using {@link #getValue()} and the exception's {@code arguments}.
 *
 * <p>Only true error / business-rule messages live here. Regular labels,
 * placeholders, and button text stay out -- those are looked up directly
 * by the view / bean.
 */
public enum MessageCode {

    // -- Auth (login + OTP) ---------------------------------------
    AUTH_EMAIL_OR_PASSWORD_REQUIRED("auth.email.or.password.required"),
    AUTH_EMAIL_OR_PASSWORD_INVALID("auth.email.or.password.invalid"),
    AUTH_ACCOUNT_INACTIVE("auth.account.inactive"),
    AUTH_SESSION_EXPIRED("auth.session.expired"),
    AUTH_OTP_INVALID_OR_EXPIRED("auth.otp.invalid.or.expired"),
    AUTH_ACCOUNT_NO_LONGER_EXISTS("auth.account.no.longer.exists"),

    // -- Registration --------------------------------------------
    REGISTER_ALL_FIELDS_REQUIRED("register.all.fields.required"),
    REGISTER_SELECT_ORGANIZATION("register.select.organization"),
    REGISTER_PASSWORDS_DO_NOT_MATCH("register.passwords.do.not.match"),
    REGISTER_PASSWORD_TOO_SHORT("register.password.too.short"),
    REGISTER_EMAIL_TAKEN("register.email.taken"),
    REGISTER_CREATE_FAILED("register.create.failed"),

    // -- Staff management ----------------------------------------
    STAFF_DELETE_FAILED("staff.delete.failed"),

    // -- Template management -------------------------------------
    TEMPLATE_UNKNOWN_ORGANIZATION("template.unknown.organization"),
    TEMPLATE_UPLOAD_NO_ACTIVE_ORGANIZATION("template.upload.no.active.organization"),
    TEMPLATE_UPLOAD_NAME_REQUIRED("template.upload.name.required"),
    TEMPLATE_UPLOAD_DOCTYPE_REQUIRED("template.upload.doctype.required"),
    TEMPLATE_UPLOAD_FILE_REQUIRED("template.upload.file.required"),
    TEMPLATE_UPLOAD_READ_FAILED("template.upload.read.failed"),
    TEMPLATE_UPLOAD_PDF_ONLY("template.upload.pdf.only"),
    TEMPLATE_UPLOAD_SAVE_FAILED("template.upload.save.failed"),
    TEMPLATE_DELETE_FAILED("template.delete.failed"),
    TEMPLATE_PREVIEW_ID_MISSING("template.preview.id.missing"),
    TEMPLATE_PREVIEW_ID_INVALID("template.preview.id.invalid"),

    // -- Resident request flow -----------------------------------
    REQUEST_LANDING_CERTIFICATE_REQUIRED("request.landing.certificate.required"),
    REQUEST_EMAIL_REQUIRED("request.email.required"),
    REQUEST_EMAIL_INVALID("request.email.invalid"),
    REQUEST_EMAIL_SEND_FAILED("request.email.send.failed"),
    REQUEST_OTP_SESSION_EXPIRED("request.otp.session.expired"),
    REQUEST_OTP_REQUIRED("request.otp.required"),
    REQUEST_OTP_INVALID_OR_EXPIRED("request.otp.invalid.or.expired"),
    REQUEST_DETAILS_REQUIRED_MISSING("request.details.required.missing"),
    REQUEST_DETAILS_OTHER_PURPOSE_REQUIRED("request.details.other.purpose.required"),
    REQUEST_SUBMIT_FAILED("request.submit.failed"),
    ;

    private final String value;

    MessageCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
