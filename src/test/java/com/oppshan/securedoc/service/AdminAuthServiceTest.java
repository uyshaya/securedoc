package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.StaffRegistrationCreate;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.exception.MessageCode;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.model.StaffOtp;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.StaffOtpRepository;
import com.oppshan.securedoc.repository.StaffRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

@QuarkusTest
class AdminAuthServiceTest {

    @Inject
    AdminAuthService adminAuthService;

    @Inject
    PasswordService passwordService;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    StaffRepository staffRepository;

    @Inject
    StaffOtpRepository staffOtpRepository;

    @Inject
    EntityManager entityManager;

    @InjectMock
    MailService mailService;

    @Test
    @Transactional
    void shouldAuthenticateWhenEmailAndPasswordMatch() {
        final var organization = seedOrganization("AUTH-OK-");
        final var email = "auth.ok+" + System.nanoTime() + "@example.test";
        final var plaintext = "horse-correct-battery";
        seedStaff(organization, email, plaintext);

        final var match = adminAuthService.authenticate(organization.getId(), email, plaintext);

        assertThat(match.isPresent(), is(true));
        assertThat(match.orElseThrow().getEmail(), is(email));
    }

    @Test
    @Transactional
    void shouldNotAuthenticateWhenPasswordWrong() {
        final var organization = seedOrganization("AUTH-WP-");
        final var email = "auth.wp+" + System.nanoTime() + "@example.test";
        seedStaff(organization, email, "right-password");

        final var match = adminAuthService.authenticate(organization.getId(), email, "wrong-password");

        assertThat(match.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldNotAuthenticateWhenEmailExistsInDifferentOrganization() {
        // The composite (org_id, email) is the authoritative login key. An
        // email valid in orgA must not authenticate against orgB.
        final var orgA = seedOrganization("AUTH-XA-");
        final var orgB = seedOrganization("AUTH-XB-");
        final var email = "alice+" + System.nanoTime() + "@example.test";
        final var plaintext = "horse-correct-battery";
        seedStaff(orgA, email, plaintext);

        final var match = adminAuthService.authenticate(orgB.getId(), email, plaintext);

        assertThat(match.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldAuthenticateInCorrectOrgWhenSameEmailExistsInTwoOrgs() {
        final var orgA = seedOrganization("AUTH-2A-");
        final var orgB = seedOrganization("AUTH-2B-");
        final var email = "alice+" + System.nanoTime() + "@example.test";
        seedStaff(orgA, email, "passwordA");
        seedStaff(orgB, email, "passwordB");

        final var matchInA = adminAuthService.authenticate(orgA.getId(), email, "passwordA");
        final var matchInB = adminAuthService.authenticate(orgB.getId(), email, "passwordB");
        final var crossA = adminAuthService.authenticate(orgA.getId(), email, "passwordB");

        assertThat(matchInA.isPresent(), is(true));
        assertThat(matchInB.isPresent(), is(true));
        assertThat(matchInA.orElseThrow().getOrganizationId(), is(orgA.getId()));
        assertThat(matchInB.orElseThrow().getOrganizationId(), is(orgB.getId()));
        assertThat(crossA.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldNotAuthenticateWhenRequiredArgumentsMissing() {
        // authenticate() declares @NotNull on orgId and @NotBlank on email +
        // password, so missing input surfaces as a method-validation
        // ConstraintViolationException rather than Optional.empty(). The
        // caller (AdminAuthBean) gates on blank input before calling the
        // service, so this is the safety net.
        final var orgId = UUID.randomUUID();
        assertThrows(ConstraintViolationException.class,
                () -> adminAuthService.authenticate(null, "hello@example.test", "anything"));
        assertThrows(ConstraintViolationException.class,
                () -> adminAuthService.authenticate(orgId, null, "anything"));
        assertThrows(ConstraintViolationException.class,
                () -> adminAuthService.authenticate(orgId, "", "anything"));
        assertThrows(ConstraintViolationException.class,
                () -> adminAuthService.authenticate(orgId, "  ", "anything"));
    }

    @Test
    @Transactional
    void shouldIssueLoginOtpAndPersistRow() {
        final var organization = seedOrganization("OTP-NEW-");
        final var staff = seedStaff(organization,
                "otp.new+" + System.nanoTime() + "@example.test", "pw");

        adminAuthService.issueLoginOtp(staff.getId());
        entityManager.flush();

        final var fresh = staffOtpRepository.findLatestUnused(staff.getId(), StaffOtp.Type.LOGIN);
        assertThat(fresh.isPresent(), is(true));
        assertThat(fresh.orElseThrow().getOtpCode().length(), is(6));
        assertThat(fresh.orElseThrow().isUsed(), is(false));
        assertThat(fresh.orElseThrow().getOtpType(), is(StaffOtp.Type.LOGIN));

        then(mailService).should(atLeastOnce()).sendStaffOtp(anyString(), anyString());
    }

    @Test
    @Transactional
    void shouldVerifyLoginOtpWhenCodeMatchesAndUnused() {
        final var organization = seedOrganization("OTP-OK-");
        final var staff = seedStaff(organization,
                "otp.ok+" + System.nanoTime() + "@example.test", "pw");
        final var code = "123456";
        final var staffOtp = persistOtp(staff, code,
                Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        entityManager.flush();

        final var ok = adminAuthService.verifyLoginOtp(staff.getId(), code);
        entityManager.flush();
        entityManager.refresh(staffOtp);

        assertThat(ok, is(true));
        assertThat(staffOtp.isUsed(), is(true));
    }

    @Test
    @Transactional
    void shouldNotVerifyLoginOtpWhenCodeIsWrong() {
        final var organization = seedOrganization("OTP-NO-");
        final var staff = seedStaff(organization,
                "otp.no+" + System.nanoTime() + "@example.test", "pw");
        persistOtp(staff, "111111",
                Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        entityManager.flush();

        final var ok = adminAuthService.verifyLoginOtp(staff.getId(), "999999");

        assertThat(ok, is(false));
    }

    @Test
    @Transactional
    void shouldNotVerifyLoginOtpAfterAttemptCapExceeded() {
        final var organization = seedOrganization("OTP-LOCK-");
        final var staff = seedStaff(organization,
                "otp.lock+" + System.nanoTime() + "@example.test", "pw");
        final var staffOtp = persistOtp(staff, "424242",
                Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        entityManager.flush();

        for (int attempt = 0; attempt < 6; attempt++) {
            adminAuthService.verifyLoginOtp(staff.getId(), "000000");
            // verifyLoginOtp merges the mutated OTP back through the active
            // EntityManager but JPA only flushes at commit. The service's
            // next call goes through the Jakarta Data stateless session and
            // queries the DB directly -- without an explicit flush each
            // iteration re-reads attempts=0 and the cap never trips.
            entityManager.flush();
        }
        entityManager.refresh(staffOtp);

        assertThat(staffOtp.isUsed(), is(true));
        assertThat(staffOtp.getOtpAttempts(), is(greaterThanOrEqualTo(5)));

        // After lockout the right code no longer succeeds.
        assertThat(adminAuthService.verifyLoginOtp(staff.getId(), "424242"), is(false));
    }

    @Test
    @Transactional
    void shouldCreateStaffWithInactiveDefault() {
        final var organization = seedOrganization("REG-OK-");
        final var form = new StaffRegistrationCreate()
                .setFirstName("New")
                .setLastName("Recruit")
                .setEmail("new.recruit+" + System.nanoTime() + "@example.test")
                .setPassword("hunter2hunter2")
                .setOrganizationId(organization.getId());

        final var view = adminAuthService.createStaff(form);
        entityManager.flush();

        assertThat(view.getId(), is(notNullValue()));
        assertThat(view.isActive(), is(false));
        assertThat(view.getEmail(), is(form.getEmail()));
        assertThat(view.getOrganizationId(), is(organization.getId()));
    }

    @Test
    @Transactional
    void shouldRaiseUnknownOrganizationWhenCreatingStaffWithMissingOrg() {
        final var form = new StaffRegistrationCreate()
                .setFirstName("Lost")
                .setLastName("Tenant")
                .setEmail("lost+" + System.nanoTime() + "@example.test")
                .setPassword("hunter2hunter2")
                .setOrganizationId(UUID.randomUUID());

        final var raised = assertThrows(BusinessException.class,
                () -> adminAuthService.createStaff(form));

        // Note: the factory shares TEMPLATE_UNKNOWN_ORGANIZATION for both unknown-org
        // and unknown-template; see BusinessException.unknownOrganization.
        assertThat(raised.getMessageCode(), is(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION));
    }

    @Test
    @Transactional
    void shouldRecordLoginByUpdatingLastLogin() {
        final var organization = seedOrganization("LST-");
        final var staff = seedStaff(organization,
                "last.login+" + System.nanoTime() + "@example.test", "pw");
        entityManager.flush();
        assertThat(staff.getLastLogin(), is(org.hamcrest.Matchers.nullValue()));

        adminAuthService.recordLogin(staff.getId());
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(staff.getLastLogin(), is(notNullValue()));
    }

    @Test
    @Transactional
    void shouldDetectEmailTakenInOrganization() {
        final var organization = seedOrganization("TKN-");
        final var email = "taken+" + System.nanoTime() + "@example.test";
        seedStaff(organization, email, "pw");
        entityManager.flush();

        assertThat(adminAuthService.emailTakenInOrganization(email, organization.getId()), is(true));
        assertThat(adminAuthService.emailTakenInOrganization(
                "free+" + System.nanoTime() + "@example.test", organization.getId()),
                is(false));
    }

    /**
     * Three back-to-back issues should leave exactly one unused row, since
     * each call's {@code invalidateActive} UPDATE retires the previous active
     * OTP. Each step runs in its own transaction via
     * {@link QuarkusTransaction#requiringNew()} -- a single outer
     * {@code @Transactional} would let MySQL's REPEATABLE_READ snapshot hide
     * the in-flight inserts from the stateless-session UPDATE, so each
     * subsequent call would see an empty active-OTP set and skip the
     * invalidation. Separate transactions force a fresh snapshot per call,
     * mirroring the real-world flow where every OTP issue is its own HTTP
     * request and its own transaction.
     */
    @Test
    void shouldInvalidatePreviousOtpsWhenIssuingNewOne() {
        final var staffId = QuarkusTransaction.requiringNew().call(() -> {
            final var organization = seedOrganization("OTP-INV-");
            return seedStaff(organization,
                    "otp.inv+" + System.nanoTime() + "@example.test", "pw").getId();
        });

        QuarkusTransaction.requiringNew().run(() -> adminAuthService.issueLoginOtp(staffId));
        QuarkusTransaction.requiringNew().run(() -> adminAuthService.issueLoginOtp(staffId));
        QuarkusTransaction.requiringNew().run(() -> adminAuthService.issueLoginOtp(staffId));

        final var unusedCount = QuarkusTransaction.requiringNew().call(() ->
                entityManager.createQuery("""
                                SELECT COUNT(otp)
                                FROM StaffOtp otp
                                WHERE otp.staff.id = :staffId AND otp.used = FALSE
                                """, Long.class)
                        .setParameter("staffId", staffId)
                        .getSingleResult());

        assertThat(unusedCount, is(1L));
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Auth Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private Staff seedStaff(Organization organization, String email, String plaintextPassword) {
        final var staff = new Staff()
                .setOrganization(organization)
                .setFirstName("Auth")
                .setLastName("Tester")
                .setEmail(email)
                .setPasswordHash(passwordService.hash(plaintextPassword))
                .setRole(Staff.Role.STAFF)
                .setActive(true);
        entityManager.persist(staff);
        // Flush so the stateless-session Jakarta Data repos the service uses
        // can see the row inside the same transaction.
        entityManager.flush();
        return staff;
    }

    private StaffOtp persistOtp(Staff staff, String code, Instant expiresAt) {
        final var staffOtp = new StaffOtp()
                .setStaff(staff)
                .setOtpCode(code)
                .setOtpType(StaffOtp.Type.LOGIN)
                .setExpiresAt(expiresAt);
        entityManager.persist(staffOtp);
        entityManager.flush();
        return staffOtp;
    }
}
