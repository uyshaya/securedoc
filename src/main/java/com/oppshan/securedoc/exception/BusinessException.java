package com.oppshan.securedoc.exception;

import java.io.Serial;

/**
 * Business-rule failure surfaced from a service to the calling bean.
 * The exception carries a symbolic {@link MessageCode} plus optional
 * {@code arguments} -- the catching layer (typically a JSF bean) resolves
 * the localized message via
 * {@link com.oppshan.securedoc.common.I18n#get(String, Object...)} using
 * {@code messageCode.getValue()} and {@link #getArguments()}.
 *
 * <p>Constructors are {@code protected}; callers go through the static
 * factory methods named after the business event
 * (e.g. {@link #unknownOrganization(Object)}).
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageCode messageCode;
    private final Object[] arguments;

    protected BusinessException(MessageCode messageCode, Object... arguments) {
        super(messageCode.getValue());
        this.messageCode = messageCode;
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    protected BusinessException(MessageCode messageCode, Throwable cause, Object... arguments) {
        super(messageCode.getValue(), cause);
        this.messageCode = messageCode;
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    // -- Auth -----------------------------------------------------

    public static BusinessException emailOrPasswordRequired() {
        return new BusinessException(MessageCode.AUTH_EMAIL_OR_PASSWORD_REQUIRED);
    }

    public static BusinessException emailOrPasswordInvalid() {
        return new BusinessException(MessageCode.AUTH_EMAIL_OR_PASSWORD_INVALID);
    }

    public static BusinessException accountInactive(Object orgLabel) {
        return new BusinessException(MessageCode.AUTH_ACCOUNT_INACTIVE, orgLabel);
    }

    public static BusinessException sessionExpired() {
        return new BusinessException(MessageCode.AUTH_SESSION_EXPIRED);
    }

    public static BusinessException otpInvalidOrExpired() {
        return new BusinessException(MessageCode.AUTH_OTP_INVALID_OR_EXPIRED);
    }

    public static BusinessException accountNoLongerExists() {
        return new BusinessException(MessageCode.AUTH_ACCOUNT_NO_LONGER_EXISTS);
    }

    // -- Registration --------------------------------------------

    public static BusinessException allFieldsRequired() {
        return new BusinessException(MessageCode.REGISTER_ALL_FIELDS_REQUIRED);
    }

    public static BusinessException selectOrganization(Object orgLabel) {
        return new BusinessException(MessageCode.REGISTER_SELECT_ORGANIZATION, orgLabel);
    }

    public static BusinessException passwordsDoNotMatch() {
        return new BusinessException(MessageCode.REGISTER_PASSWORDS_DO_NOT_MATCH);
    }

    public static BusinessException passwordTooShort(int minimumLength) {
        return new BusinessException(MessageCode.REGISTER_PASSWORD_TOO_SHORT, minimumLength);
    }

    public static BusinessException emailTaken(Object orgLabel) {
        return new BusinessException(MessageCode.REGISTER_EMAIL_TAKEN, orgLabel);
    }

    public static BusinessException createAccountFailed(Throwable cause, Object detail) {
        return new BusinessException(MessageCode.REGISTER_CREATE_FAILED, cause, detail);
    }

    // -- Unknown-organization / unknown-template (services) -----
    //
    // Both throw sites currently share {@code template.unknown.organization}
    // (and a re-used variant for "unknown template" since messages.properties
    // is being edited by a parallel agent during this sweep). When that
    // settles, callers should split these into dedicated keys.

    public static BusinessException unknownOrganization(Object organizationId) {
        return new BusinessException(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION, organizationId);
    }

    public static BusinessException unknownTemplate(Object templateId) {
        return new BusinessException(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION, templateId);
    }

    // -- Staff management ----------------------------------------

    public static BusinessException staffDeleteFailed(Throwable cause, Object staffName) {
        return new BusinessException(MessageCode.STAFF_DELETE_FAILED, cause, staffName);
    }

    // -- Template management -------------------------------------

    public static BusinessException templateUploadNoActiveOrganization() {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_NO_ACTIVE_ORGANIZATION);
    }

    public static BusinessException templateUploadNameRequired() {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_NAME_REQUIRED);
    }

    public static BusinessException templateUploadDocTypeRequired() {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_DOCTYPE_REQUIRED);
    }

    public static BusinessException templateUploadFileRequired() {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_FILE_REQUIRED);
    }

    public static BusinessException templateUploadReadFailed(Throwable cause, Object detail) {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_READ_FAILED, cause, detail);
    }

    public static BusinessException templateUploadPdfOnly() {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_PDF_ONLY);
    }

    public static BusinessException templateUploadSaveFailed(Throwable cause, Object detail) {
        return new BusinessException(MessageCode.TEMPLATE_UPLOAD_SAVE_FAILED, cause, detail);
    }

    public static BusinessException templateDeleteFailed(Throwable cause, Object templateName, Object detail) {
        return new BusinessException(MessageCode.TEMPLATE_DELETE_FAILED, cause, templateName, detail);
    }

    public static BusinessException templatePreviewIdMissing() {
        return new BusinessException(MessageCode.TEMPLATE_PREVIEW_ID_MISSING);
    }

    public static BusinessException templatePreviewIdInvalid() {
        return new BusinessException(MessageCode.TEMPLATE_PREVIEW_ID_INVALID);
    }

    // -- Resident request flow -----------------------------------

    public static BusinessException landingOrganizationRequired(Object orgLabel) {
        return new BusinessException(MessageCode.REQUEST_LANDING_ORGANIZATION_REQUIRED, orgLabel);
    }

    public static BusinessException landingCertificateRequired() {
        return new BusinessException(MessageCode.REQUEST_LANDING_CERTIFICATE_REQUIRED);
    }

    public static BusinessException requestEmailRequired() {
        return new BusinessException(MessageCode.REQUEST_EMAIL_REQUIRED);
    }

    public static BusinessException requestEmailInvalid() {
        return new BusinessException(MessageCode.REQUEST_EMAIL_INVALID);
    }

    public static BusinessException requestEmailSendFailed(Throwable cause, Object detail) {
        return new BusinessException(MessageCode.REQUEST_EMAIL_SEND_FAILED, cause, detail);
    }

    public static BusinessException requestOtpSessionExpired() {
        return new BusinessException(MessageCode.REQUEST_OTP_SESSION_EXPIRED);
    }

    public static BusinessException requestOtpRequired() {
        return new BusinessException(MessageCode.REQUEST_OTP_REQUIRED);
    }

    public static BusinessException requestOtpInvalidOrExpired() {
        return new BusinessException(MessageCode.REQUEST_OTP_INVALID_OR_EXPIRED);
    }

    public static BusinessException requestDetailsRequiredMissing() {
        return new BusinessException(MessageCode.REQUEST_DETAILS_REQUIRED_MISSING);
    }

    public static BusinessException requestDetailsOtherPurposeRequired() {
        return new BusinessException(MessageCode.REQUEST_DETAILS_OTHER_PURPOSE_REQUIRED);
    }

    public static BusinessException requestSubmitFailed(Throwable cause, Object detail) {
        return new BusinessException(MessageCode.REQUEST_SUBMIT_FAILED, cause, detail);
    }

    public MessageCode getMessageCode() {
        return messageCode;
    }

    public Object[] getArguments() {
        return arguments;
    }
}
