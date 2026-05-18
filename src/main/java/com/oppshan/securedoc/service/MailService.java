package com.oppshan.securedoc.service;

/**
 * Outbound mail seam. Default implementation logs to stdout so the rest
 * of the auth flow is testable without an SMTP server. Swap for a real
 * SMTP-backed implementation (e.g. quarkus-mailer) later -- callers
 * inject {@code MailService}, not the concrete class.
 */
public interface MailService {

    void sendStaffOtp(String email, String code);

    void sendResidentOtp(String email, String code);
}
