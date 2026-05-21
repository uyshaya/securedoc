package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Requester;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Repository-layer integration test for {@link RequesterRepository}.
 * {@code RequesterRepository} only declares the inherited CRUD methods, so
 * the round-trip is the canonical proof that {@code @UuidGenerator(VERSION_7)}
 * fires, the audit listener stamps the timestamps, and the {@code sex} ENUM
 * column accepts {@code 'M'}/{@code 'F'}.
 */
@QuarkusTest
class RequesterRepositoryTest {

    @Inject
    RequesterRepository requesterRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldRoundTripRequesterThroughInsertAndFindById() {
        final var requester = new Requester()
                .setFirstName("Juan")
                .setMiddleName("Garcia")
                .setLastName("Dela Cruz")
                .setEmail("juan.delacruz+" + System.nanoTime() + "@example.test")
                .setSex("M")
                .setDateOfBirth(LocalDate.of(1990, 1, 15))
                .setContactNumber("09171234567")
                .setIdType("Drivers License")
                .setIdImageData(new byte[]{1, 2, 3, 4, 5});

        requesterRepository.insertWithSession(requester);
        entityManager.flush();

        assertThat("@UuidGenerator should have populated the PK",
                requester.getId(), is(notNullValue()));
        assertThat("Standard UUID string is 36 chars",
                requester.getId().toString().length(), is(36));
        assertThat("AuditableEntityEntityListener @PrePersist must set createdAt",
                requester.getCreatedAt(), is(notNullValue()));
        assertThat("AuditableEntityEntityListener @PrePersist must set lastModifiedAt",
                requester.getLastModifiedAt(), is(notNullValue()));

        final var reloaded = requesterRepository.findById(requester.getId()).orElseThrow();
        assertThat(reloaded.getFirstName(), is("Juan"));
        assertThat(reloaded.getMiddleName(), is("Garcia"));
        assertThat(reloaded.getLastName(), is("Dela Cruz"));
        assertThat(reloaded.getEmail(), is(requester.getEmail()));
        assertThat(reloaded.getSex(), is("M"));
        assertThat(reloaded.getDateOfBirth(), is(LocalDate.of(1990, 1, 15)));
        assertThat(reloaded.getContactNumber(), is("09171234567"));
        assertThat(reloaded.getIdType(), is("Drivers License"));
        assertThat(reloaded.getIdImageData(), is(new byte[]{1, 2, 3, 4, 5}));
    }
}
