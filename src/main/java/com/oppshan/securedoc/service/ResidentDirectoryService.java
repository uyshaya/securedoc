package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.ResidentView;
import com.oppshan.securedoc.model.Resident;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.ResidentRepository;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-organization resident directory.
 *
 * <p>The read surface ({@link #listForOrganization}, {@link #countForOrganization})
 * is what the rest of the app calls and is meant to outlive the CSV
 * bootstrap path. {@link #replaceFromCsv} and {@link #clearForOrganization}
 * exist only because the target barangay has no upstream resident-records
 * system yet; when one lands, those write methods + the {@code resident}
 * table are deleted and the read methods swap to remote lookups.
 */
@ApplicationScoped
public class ResidentDirectoryService {

    static final List<String> EXPECTED_HEADERS = List.of(
            "first_name",
            "middle_name",
            "last_name",
            "sex",
            "date_of_birth",
            "address"
    );

    private final ResidentRepository residentRepo;

    private final OrganizationRepository organizationRepo;

    private final Logger logger;

    @Inject
    public ResidentDirectoryService(ResidentRepository residentRepo,
                                    OrganizationRepository organizationRepo,
                                    Logger logger) {
        this.residentRepo = residentRepo;
        this.organizationRepo = organizationRepo;
        this.logger = logger;
    }

    @Transactional
    public List<ResidentView> listForOrganization(@Nullable UUID organizationId) {
        logger.tracef("Listing residents in organization %s", organizationId);
        if (organizationId == null) {
            return List.of();
        }

        return residentRepo.listByOrganization(organizationId).stream()
                .map(Resident::toView)
                .toList();
    }

    @Transactional
    public long countForOrganization(@Nullable UUID organizationId) {
        if (organizationId == null) {
            return 0L;
        }

        return residentRepo.countByOrganization(organizationId);
    }

    /**
     * Identity match used by the requests detail view: case-insensitive
     * on first + last name, exact on DOB. Returns the first matching
     * masterlist row (empty if none, or if any required argument is
     * missing). Callers treat the result as advisory -- a "not on
     * masterlist" answer should warn staff, not block approval.
     */
    @Transactional
    public Optional<ResidentView> findMatchForRequester(@Nullable UUID organizationId,
                                                       @Nullable String firstName,
                                                       @Nullable String lastName,
                                                       @Nullable LocalDate dateOfBirth) {
        if (organizationId == null
                || firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()
                || dateOfBirth == null) {
            return Optional.empty();
        }

        return residentRepo.findMatching(organizationId, firstName.trim(), lastName.trim(), dateOfBirth)
                .stream()
                .findFirst()
                .map(Resident::toView);
    }

    @Transactional
    public void clearForOrganization(@Nullable UUID organizationId) {
        logger.tracef("Clearing resident directory for organization %s", organizationId);
        if (organizationId == null) {
            return;
        }

        residentRepo.deleteByOrganization(organizationId);
        logger.debugf("Cleared resident directory for organization %s", organizationId);
    }

    /**
     * Replace-all import. Parses the CSV stream, validates every row, and
     * only mutates the table when the whole file is valid -- a row-level
     * error aborts the import with no partial writes.
     */
    @Transactional
    public ImportResult replaceFromCsv(@NotNull UUID organizationId, @NotNull InputStream csvStream) {
        logger.tracef("Importing resident CSV into organization %s", organizationId);
        final var organization = organizationRepo.findById(organizationId).orElse(null);
        if (organization == null) {
            logger.debugf("Resident CSV import rejected -- organization %s not found", organizationId);
            return ImportResult.failure(List.of("Unknown organization: " + organizationId));
        }

        final List<String[]> rows;
        try {
            rows = parseCsv(csvStream);
        } catch (IOException readFailure) {
            logger.warnf(readFailure, "Failed to read resident CSV for organization %s", organizationId);
            return ImportResult.failure(List.of("Could not read CSV: " + readFailure.getMessage()));
        }

        if (rows.isEmpty()) {
            return ImportResult.failure(List.of("CSV is empty."));
        }

        final var header = rows.getFirst();
        final var headerError = validateHeader(header);
        if (headerError != null) {
            return ImportResult.failure(List.of(headerError));
        }

        final var parsed = new ArrayList<Resident>(rows.size() - 1);
        final var errors = new ArrayList<String>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            final var row = rows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            try {
                parsed.add(buildResident(organization, row, rowIndex + 1));
            } catch (CsvRowException invalid) {
                errors.add(invalid.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            return ImportResult.failure(errors);
        }

        residentRepo.deleteByOrganization(organizationId);
        for (final var resident : parsed) {
            residentRepo.insertWithSession(resident);
        }

        logger.debugf("Imported %d residents into organization %s", parsed.size(), organizationId);
        return ImportResult.success(parsed.size());
    }

    @Nullable
    private static String validateHeader(String[] header) {
        if (header.length < EXPECTED_HEADERS.size()) {
            return "CSV header is missing required columns. Expected: " + String.join(", ", EXPECTED_HEADERS);
        }

        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            final var actual = header[columnIndex].trim().toLowerCase(Locale.ROOT);
            final var expected = EXPECTED_HEADERS.get(columnIndex);
            if (!expected.equals(actual)) {
                return "CSV column " + (columnIndex + 1) + " must be '" + expected
                        + "' but was '" + header[columnIndex] + "'.";
            }
        }

        return null;
    }

    private static boolean isBlankRow(String[] row) {
        for (final var cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }

        return true;
    }

    private static Resident buildResident(com.oppshan.securedoc.model.Organization organization,
                                          String[] row,
                                          int lineNumber) {
        if (row.length < EXPECTED_HEADERS.size()) {
            throw new CsvRowException("Line " + lineNumber + ": expected "
                    + EXPECTED_HEADERS.size() + " columns, got " + row.length + ".");
        }

        final var firstName = require(row[0], "first_name", lineNumber);
        final var middleName = optional(row[1]);
        final var lastName = require(row[2], "last_name", lineNumber);
        final var sex = parseSex(row[3], lineNumber);
        final var dateOfBirth = parseDate(row[4], lineNumber);
        final var address = require(row[5], "address", lineNumber);

        return new Resident()
                .setOrganization(organization)
                .setFirstName(firstName)
                .setMiddleName(middleName)
                .setLastName(lastName)
                .setSex(sex)
                .setDateOfBirth(dateOfBirth)
                .setAddress(address);
    }

    private static String require(String value, String columnName, int lineNumber) {
        if (value == null || value.isBlank()) {
            throw new CsvRowException("Line " + lineNumber + ": '" + columnName + "' is required.");
        }

        return value.trim();
    }

    @Nullable
    private static String optional(@Nullable String value) {
        if (value == null) {
            return null;
        }

        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String parseSex(String value, int lineNumber) {
        final var normalized = require(value, "sex", lineNumber).toUpperCase(Locale.ROOT);
        if (normalized.equals("M") || normalized.equals("MALE")) {
            return "M";
        }

        if (normalized.equals("F") || normalized.equals("FEMALE")) {
            return "F";
        }

        throw new CsvRowException("Line " + lineNumber + ": 'sex' must be M or F (got '" + value + "').");
    }

    private static LocalDate parseDate(String value, int lineNumber) {
        final var trimmed = require(value, "date_of_birth", lineNumber);
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException invalid) {
            throw new CsvRowException("Line " + lineNumber
                    + ": 'date_of_birth' must be ISO yyyy-MM-dd (got '" + value + "').");
        }
    }

    /**
     * Minimal RFC-4180-style CSV parser. Supports double-quoted fields that
     * may contain commas, embedded newlines, and {@code ""} as an escaped
     * double quote. The full files we expect here are small (a barangay
     * masterlist), so the whole stream is buffered before parsing.
     */
    static List<String[]> parseCsv(InputStream csvStream) throws IOException {
        final var rows = new ArrayList<String[]>();
        final var currentRow = new ArrayList<String>();
        final var currentField = new StringBuilder();
        boolean insideQuotes = false;

        try (final var reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            int nextChar = reader.read();
            // Strip UTF-8 BOM if present so the first header column matches.
            if (nextChar == 0xFEFF) {
                nextChar = reader.read();
            }

            while (nextChar != -1) {
                final char character = (char) nextChar;

                if (insideQuotes) {
                    if (character == '"') {
                        final int peek = reader.read();
                        if (peek == '"') {
                            currentField.append('"');
                        } else {
                            insideQuotes = false;
                            nextChar = peek;
                            continue;
                        }
                    } else {
                        currentField.append(character);
                    }
                } else {
                    if (character == '"' && currentField.length() == 0) {
                        insideQuotes = true;
                    } else if (character == ',') {
                        currentRow.add(currentField.toString());
                        currentField.setLength(0);
                    } else if (character == '\r') {
                        // Swallow; the paired '\n' will close the row.
                    } else if (character == '\n') {
                        currentRow.add(currentField.toString());
                        currentField.setLength(0);
                        rows.add(currentRow.toArray(new String[0]));
                        currentRow.clear();
                    } else {
                        currentField.append(character);
                    }
                }

                nextChar = reader.read();
            }
        }

        // Final field / row if the file didn't end with a newline.
        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentField.toString());
            rows.add(currentRow.toArray(new String[0]));
        }

        return rows;
    }

    public static final class ImportResult implements Serializable {

        private static final long serialVersionUID = 7283910472638192745L;

        private final int imported;
        private final List<String> errors;

        private ImportResult(int imported, List<String> errors) {
            this.imported = imported;
            this.errors = errors;
        }

        public static ImportResult success(int imported) {
            return new ImportResult(imported, List.of());
        }

        public static ImportResult failure(List<String> errors) {
            return new ImportResult(0, List.copyOf(errors));
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }

        public int getImported() {
            return imported;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private static final class CsvRowException extends RuntimeException {

        private static final long serialVersionUID = 4172839102837465L;

        CsvRowException(String message) {
            super(message);
        }
    }
}
