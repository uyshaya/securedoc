package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.ResidentOtp;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Repository-layer integration test for {@link ResidentOtpRepository}.
 * Mirrors {@link StaffOtpRepositoryTest} structurally minus the
 * staff/type discriminator -- residents share one OTP flow keyed by email.
 * The repository's two JPQL methods are {@code findLatestUnused(email)}
 * and {@code invalidateActive(email)}.
 */
@QuarkusTest
class ResidentOtpRepositoryTest {

    @Inject
    ResidentOtpRepository residentOtpRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldFindLatestUnusedReturningMostRecentOtpForEmail() {
        final var email = "resident.otp+" + System.nanoTime() + "@example.test";

        final var older = newResidentOtp(email, "111111");
        residentOtpRepository.insertWithSession(older);
        entityManager.flush();

        final var newer = newResidentOtp(email, "222222");
        residentOtpRepository.insertWithSession(newer);
        entityManager.flush();

        final var latest = residentOtpRepository.findLatestUnused(email);

        assertThat(latest.isPresent(), is(true));
        // ORDER BY id DESC + v7 UUIDs => the second-inserted row wins.
        assertThat(latest.orElseThrow().getId(), is(newer.getId()));
        assertThat(latest.orElseThrow().getOtpCode(), is("222222"));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromFindLatestUnusedWhenAllUsed() {
        final var email = "all.used+" + System.nanoTime() + "@example.test";
        final var used = newResidentOtp(email, "333333").setUsed(true);
        residentOtpRepository.insertWithSession(used);
        entityManager.flush();

        final var latest = residentOtpRepository.findLatestUnused(email);

        assertThat(latest.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldInvalidateActiveByMarkingAllUnusedRowsUsed() {
        final var email = "invalidate+" + System.nanoTime() + "@example.test";
        residentOtpRepository.insertWithSession(newResidentOtp(email, "444444"));
        residentOtpRepository.insertWithSession(newResidentOtp(email, "555555"));
        entityManager.flush();

        final var rowsInvalidated = residentOtpRepository.invalidateActive(email);
        entityManager.flush();

        assertThat(rowsInvalidated, is(2));

        final var stillActive = residentOtpRepository.findLatestUnused(email);
        assertThat(stillActive.isEmpty(), is(true));
    }

    private static ResidentOtp newResidentOtp(String email, String code) {
        return new ResidentOtp()
                .setEmail(email)
                .setOtpCode(code)
                .setOtpAttempts(0)
                .setUsed(false)
                .setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    }
}
