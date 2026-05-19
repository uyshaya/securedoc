package com.oppshan.securedoc.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Unit-style test for {@link PasswordService}. The service is
 * {@code @ApplicationScoped} so we boot Quarkus to get the CDI proxy with
 * the constructor-injected {@code Logger} wired -- avoids the need to
 * hand-instantiate with a null logger and dodge the trace calls.
 */
@QuarkusTest
class PasswordServiceTest {

    @Inject
    PasswordService passwordService;

    @Test
    void shouldHashPasswordToBcryptFormat() {
        final var hash = passwordService.hash("correct-horse-battery-staple");

        assertThat(hash, is(notNullValue()));
        // BouncyCastle's OpenBSDBCrypt emits the $2y$ prefix variant.
        assertThat(hash, startsWith("$2y$"));
    }

    @Test
    void shouldVerifyCorrectPassword() {
        final var plaintext = "correct-horse-battery-staple";
        final var hash = passwordService.hash(plaintext);

        assertThat(passwordService.verify(plaintext, hash), is(true));
    }

    @Test
    void shouldNotVerifyWrongPassword() {
        final var hash = passwordService.hash("correct-horse-battery-staple");

        assertThat(passwordService.verify("not-the-right-password", hash), is(false));
    }

    @Test
    void shouldNotVerifyWhenHashIsMalformed() {
        assertThat(passwordService.verify("anything", "not-a-valid-bcrypt-hash"), is(false));
    }
}
