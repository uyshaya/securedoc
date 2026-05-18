package com.oppshan.securedoc.migration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end migration smoke test against a real MySQL 8 container spun up by Quarkus DevServices
 * ({@code %test.quarkus.datasource.devservices.*}).
 *
 * <p>Asserts every versioned migration on the classpath applied with
 * {@link MigrationState#SUCCESS} and the pending list is empty. The classpath-vs-applied count check scales
 * automatically: adding V3 / V4 / ... doesn't require touching this test.
 *
 * <p>Also asserts the singularized table names from V2 are in place (and the
 * old plural names are gone), which catches a half-applied V2 where some {@code RENAME TABLE} calls ran and others
 * didn't.
 *
 * <p>Hibernate's {@code schema-management.strategy=validate} is enforced just
 * by Quarkus completing startup -- if entity mappings disagreed with the migrated schema, Quarkus would have refused to
 * boot before any test method ran.
 */
@QuarkusTest
class FlywayMigrationTest {

    private static final String MIGRATIONS_CLASSPATH_LOCATION = "db/migration/mysql";

    private static final Pattern VERSIONED_MIGRATION_FILENAME = Pattern.compile("^V\\d+__.+\\.sql$");

    private static final String COUNT_TABLES_BY_NAME = """
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = :name
            """;

    @Inject
    Flyway flyway;

    @Inject
    EntityManager entityManager;

    @Test
    void shouldHaveAppliedEveryVersionedMigrationSuccessfully() throws IOException {
        final var classpathFilenames = listClasspathVersionedMigrationFilenames();
        final var info = flyway.info();
        final var appliedMigrations = Arrays.asList(info.applied());
        final var pendingMigrations = Arrays.asList(info.pending());

        assertThat("Versioned migration files must exist on the classpath",
                classpathFilenames.size(), is(greaterThan(0)));

        assertThat("No migration may be pending after migrate-at-start",
                pendingMigrations, is(empty()));

        assertThat("Applied migration count must match versioned files on the classpath",
                appliedMigrations.size(), is(equalTo(classpathFilenames.size())));

        for (final var migration : appliedMigrations) {
            final var label = "V" + migration.getVersion() + "__" + migration.getDescription();
            assertThat(label + " must have SUCCESS state",
                    migration.getState(), is(equalTo(MigrationState.SUCCESS)));
        }
    }

    @Test
    @Transactional
    void shouldHaveSingularizedTableNames() {
        assertTableExists("organization");
        assertTableExists("staff");
        assertTableExists("staff_otp");
        assertTableExists("document_template");
        assertTableExists("request");
        assertTableExists("document");
        assertTableExists("audit_log");

        assertTableMissing("organizations");
        assertTableMissing("staff_otps");
        assertTableMissing("document_templates");
        assertTableMissing("requests");
        assertTableMissing("documents");
        assertTableMissing("audit_logs");
    }

    private static List<String> listClasspathVersionedMigrationFilenames() throws IOException {
        final var resourceUrl = FlywayMigrationTest.class.getClassLoader()
                .getResource(MIGRATIONS_CLASSPATH_LOCATION);
        if (resourceUrl == null) {
            throw new IllegalStateException(
                    "Classpath resource " + MIGRATIONS_CLASSPATH_LOCATION + " is missing");
        }

        try (final var stream = Files.list(Path.of(URI.create(resourceUrl.toString())))) {
            return stream
                    .map(path -> path.getFileName().toString())
                    .filter(name -> VERSIONED_MIGRATION_FILENAME.matcher(name).matches())
                    .sorted()
                    .toList();
        }
    }

    private void assertTableExists(String tableName) {
        assertEquals(1L, countTablesNamed(tableName),
                "Expected table '" + tableName + "' to exist after V2");
    }

    private void assertTableMissing(String tableName) {
        assertEquals(0L, countTablesNamed(tableName),
                "Expected table '" + tableName + "' to have been renamed away by V2");
    }

    private long countTablesNamed(String tableName) {
        return entityManager.unwrap(Session.class)
                .createNativeQuery(COUNT_TABLES_BY_NAME, Long.class)
                .setParameter("name", tableName)
                .getSingleResult();
    }
}
