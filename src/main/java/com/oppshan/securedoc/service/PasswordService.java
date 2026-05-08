package com.oppshan.securedoc.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import java.security.SecureRandom;

/**
 * BCrypt-based password hashing via BouncyCastle's {@link OpenBSDBCrypt}.
 * Stored hashes use the standard {@code $2y$<cost>$<salt+hash>} format,
 * so they can be read by any other BCrypt-compatible verifier.
 */
@ApplicationScoped
public class PasswordService {

    /**
     * BCrypt cost factor. 12 ≈ ~250ms on modern hardware in 2026.
     * Bump every couple of years as compute gets cheaper.
     */
    private static final int COST = 12;

    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    public String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return OpenBSDBCrypt.generate(plaintext.toCharArray(), salt, COST);
    }

    public boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return OpenBSDBCrypt.checkPassword(storedHash, plaintext.toCharArray());
        } catch (RuntimeException e) {
            // Malformed stored hash, etc. Treat as failed verification.
            return false;
        }
    }
}
