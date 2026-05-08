package com.oppshan.securedoc.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Default {@link MailService} implementation backed by Quarkus Mailer.
 *
 * <p>In dev mode, Quarkus auto-mocks the mailer (mails are captured
 * and visible at {@code /q/dev}, not sent over the wire) — toggle
 * {@code %dev.quarkus.mailer.mock=false} to actually send while in dev.
 *
 * <p>For production, set {@code quarkus.mailer.host} / {@code port} /
 * {@code username} / {@code password} (use env vars; see
 * {@code application.properties}).
 */
@ApplicationScoped
public class SmtpMailService implements MailService {

    private static final Logger LOG = Logger.getLogger(SmtpMailService.class);

    @Inject
    Mailer mailer;

    @Override
    public void sendStaffOtp(String email, String code) {
        String subject = "SecureDoc — Your sign-in verification code";
        String body =
                "Your one-time sign-in code is: " + code + "\n\n" +
                "This code expires in 5 minutes. If you didn't try to sign in, " +
                "you can safely ignore this email.\n\n" +
                "— SecureDoc";
        mailer.send(Mail.withText(email, subject, body));
        LOG.infof("Dispatched staff OTP to %s", email);
    }

    @Override
    public void sendResidentOtp(String email, String code) {
        String subject = "SecureDoc — Your verification code";
        String body =
                "Your verification code is: " + code + "\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "— SecureDoc";
        mailer.send(Mail.withText(email, subject, body));
        LOG.infof("Dispatched resident OTP to %s", email);
    }
}
