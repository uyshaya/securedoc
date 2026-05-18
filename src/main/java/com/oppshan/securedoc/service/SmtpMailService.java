package com.oppshan.securedoc.service;

import com.oppshan.securedoc.common.I18n;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Default {@link MailService} implementation backed by Quarkus Mailer.
 *
 * <p>In dev mode, Quarkus auto-mocks the mailer (mails are captured
 * and visible at {@code /q/dev}, not sent over the wire) -- toggle
 * {@code %dev.quarkus.mailer.mock=false} to actually send while in dev.
 *
 * <p>For production, set {@code quarkus.mailer.host} / {@code port} /
 * {@code username} / {@code password} (use env vars; see
 * {@code application.properties}).
 *
 * <p>Subject / body content is sourced from the {@code messages} resource
 * bundle via {@link I18n} so future locales can be added without
 * touching this service.
 */
@ApplicationScoped
public class SmtpMailService implements MailService {

    private static final Logger LOG = Logger.getLogger(SmtpMailService.class);

    private final Mailer mailer;
    private final I18n i18n;

    @Inject
    public SmtpMailService(Mailer mailer, I18n i18n) {
        this.mailer = mailer;
        this.i18n = i18n;
    }

    @Override
    public void sendStaffOtp(String email, String code) {
        final var subject = i18n.get("mail.staff.otp.subject");
        final var body = i18n.get("mail.staff.otp.body", code);
        mailer.send(Mail.withText(email, subject, body));
        LOG.infof("Dispatched staff OTP to %s", email);
    }

    @Override
    public void sendResidentOtp(String email, String code) {
        final var subject = i18n.get("mail.resident.otp.subject");
        final var body = i18n.get("mail.resident.otp.body", code);
        mailer.send(Mail.withText(email, subject, body));
        LOG.infof("Dispatched resident OTP to %s", email);
    }
}
