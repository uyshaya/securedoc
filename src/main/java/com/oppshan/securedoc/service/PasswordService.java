package com.oppshan.securedoc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.jboss.logging.Logger;

import java.security.SecureRandom;

/**
 * BCrypt-based password hashing via BouncyCastle's {@link OpenBSDBCrypt}.
 * Stored hashes use the standard {@code $2y$<cost>$<salt+hash>} format,
 * so they can be read by any other BCrypt-compatible verifier.
 */
@ApplicationScoped
public class PasswordService {

    /**
     * BCrypt cost factor. 12 ~ ~250ms on modern hardware in 2026.
     * Bump every couple of years as compute gets cheaper.
     */
    private static final int COST = 12;

    private static final int SALT_BYTES = 16;

    private final Logger logger;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public PasswordService(Logger logger) {
        this.logger = logger;
    }

    protected PasswordService() {
        this(null);
    }

    public String hash(@NotBlank String plaintext) {
        logger.tracef("Hashing password (plaintext present: %s)", plaintext != null);
        final var salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return OpenBSDBCrypt.generate(plaintext.toCharArray(), salt, COST);
    }

    public boolean verify(@NotBlank String plaintext, @NotNull String storedHash) {
        logger.tracef("Verifying password (plaintext present: %s, stored hash present: %s)",
                plaintext != null, storedHash != null && !storedHash.isEmpty());
        if (storedHash.isEmpty()) {
            return false;
        }

        try {
            return OpenBSDBCrypt.checkPassword(storedHash, plaintext.toCharArray());
        } catch (RuntimeException verifyFailure) {
            // Malformed stored hash, etc. Treat as failed verification.
            logger.warnf(verifyFailure, "Password verification failed -- stored hash is malformed");
            return false;
        }
    }
}
