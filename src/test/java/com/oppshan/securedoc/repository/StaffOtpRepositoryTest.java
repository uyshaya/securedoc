package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.model.StaffOtp;
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
 * Repository-layer integration test for {@link StaffOtpRepository}. The
 * tests exercise {@code findLatestUnused} (covered by
 * {@code idx_staff_otps_lookup}) and {@code invalidateActive}; they seed
 * an org + staff up front and then several OTPs per test as needed.
 */
@QuarkusTest
class StaffOtpRepositoryTest {

    @Inject
    StaffOtpRepository staffOtpRepository;

    @Inject
    StaffRepository staffRepository;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldFindLatestUnusedReturningMostRecentOtp() {
        final var staff = seedStaff();

        final var older = newStaffOtp(staff, "111111", StaffOtp.Type.LOGIN);
        staffOtpRepository.insertWithSession(older);
        entityManager.flush();

        final var newer = newStaffOtp(staff, "222222", StaffOtp.Type.LOGIN);
        staffOtpRepository.insertWithSession(newer);
        entityManager.flush();

        final var latest = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.LOGIN);

        assertThat(latest.isPresent(), is(true));
        // The repository ORDERs BY id DESC, and v7 UUIDs are time-ordered,
        // so the second-inserted row wins regardless of clock skew.
        assertThat(latest.orElseThrow().getId(), is(newer.getId()));
        assertThat(latest.orElseThrow().getOtpCode(), is("222222"));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromFindLatestUnusedWhenAllUsed() {
        final var staff = seedStaff();

        final var used = newStaffOtp(staff, "333333", StaffOtp.Type.LOGIN).setUsed(true);
        staffOtpRepository.insertWithSession(used);
        entityManager.flush();

        final var latest = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.LOGIN);

        assertThat(latest.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldInvalidateActiveByMarkingAllUnusedRowsUsed() {
        final var staff = seedStaff();

        final var first = newStaffOtp(staff, "444444", StaffOtp.Type.LOGIN);
        final var second = newStaffOtp(staff, "555555", StaffOtp.Type.LOGIN);
        staffOtpRepository.insertWithSession(first);
        staffOtpRepository.insertWithSession(second);
        entityManager.flush();

        final var rowsInvalidated = staffOtpRepository.invalidateActive(staff.getId(), StaffOtp.Type.LOGIN);
        entityManager.flush();

        assertThat(rowsInvalidated, is(2));

        final var stillActive = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.LOGIN);
        assertThat(stillActive.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldRespectOtpTypeScopingInFindLatestUnused() {
        final var staff = seedStaff();

        final var loginOtp = newStaffOtp(staff, "666666", StaffOtp.Type.LOGIN);
        final var resetOtp = newStaffOtp(staff, "777777", StaffOtp.Type.PASSWORD_RESET);
        staffOtpRepository.insertWithSession(loginOtp);
        staffOtpRepository.insertWithSession(resetOtp);
        entityManager.flush();

        final var login = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.LOGIN);
        final var reset = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.PASSWORD_RESET);

        assertThat(login.isPresent(), is(true));
        assertThat(login.orElseThrow().getOtpType(), is(StaffOtp.Type.LOGIN));
        assertThat(login.orElseThrow().getOtpCode(), is("666666"));

        assertThat(reset.isPresent(), is(true));
        assertThat(reset.orElseThrow().getOtpType(), is(StaffOtp.Type.PASSWORD_RESET));
        assertThat(reset.orElseThrow().getOtpCode(), is("777777"));
    }

    private Staff seedStaff() {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("OTP Test Barangay")
                .setCode("OTP-" + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);

        final var staff = new Staff()
                .setOrganization(organization)
                .setFirstName("Otp")
                .setLastName("Holder")
                .setEmail("otp.holder+" + System.nanoTime() + "@example.test")
                .setPasswordHash("$2a$10$placeholderhashvaluefortestsxxxxxxxxxxxxxxxxxxxxxxx")
                .setRole(Staff.Role.STAFF)
                .setActive(true);
        staffRepository.insertWithSession(staff);
        entityManager.flush();
        return staff;
    }

    private static StaffOtp newStaffOtp(Staff staff, String code, StaffOtp.Type type) {
        return new StaffOtp()
                .setStaff(staff)
                .setOtpCode(code)
                .setOtpType(type)
                .setOtpAttempts(0)
                .setUsed(false)
                .setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    }
}
