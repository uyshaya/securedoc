package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import com.oppshan.securedoc.dto.ResidentView;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A row in the per-organization resident masterlist. Populated by CSV
 * upload via {@code /admin/residents/...} as a stand-in while the
 * target barangay has no upstream resident-records system. Distinct
 * from {@link Requester}, which is a per-submission snapshot of the
 * applicant -- a Resident row is the directory entry, never linked
 * to a {@link Request}.
 *
 * <p>When an upstream resident-records API exists, this entity and
 * the {@code resident} table can be dropped; the lookups exposed by
 * {@code ResidentDirectoryService} are the only surface meant to
 * outlive the bootstrap CSV path.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "resident",
        indexes = {
                @Index(
                        name = "idx_resident_organization_lastname_firstname",
                        columnList = "organization_id, last_name, first_name"
                )
        }
)
public class Resident
        implements AuditableEntity<Resident>, Serializable {

    @Serial
    private static final long serialVersionUID = 5917823649102837465L;

    @Id
    @Basic(optional = false)
    @Column(name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL")
    @JdbcTypeCode(SqlTypes.CHAR)
    @UuidGenerator(style = Style.VERSION_7)
    @NotNull
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL",
            foreignKey = @ForeignKey(name = "fk_resident_organization"))
    @NotNull
    private Organization organization;

    @Basic(optional = false)
    @Column(name = "first_name",
            nullable = false,
            length = 100)
    @NotEmpty
    private String firstName;

    @Column(name = "middle_name",
            length = 100)
    @Nullable
    private String middleName;

    @Basic(optional = false)
    @Column(name = "last_name",
            nullable = false,
            length = 100)
    @NotEmpty
    private String lastName;

    @Basic(optional = false)
    @Column(name = "sex",
            nullable = false,
            columnDefinition = "ENUM('M','F') NOT NULL",
            length = 1)
    @NotEmpty
    private String sex;

    @Basic(optional = false)
    @Column(name = "date_of_birth",
            nullable = false)
    @NotNull
    private LocalDate dateOfBirth;

    @Basic(optional = false)
    @Column(name = "address",
            nullable = false,
            length = 500)
    @NotEmpty
    private String address;

    @Basic(optional = false)
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    @NotNull
    private Instant createdAt;

    @Basic(optional = false)
    @Column(name = "updated_at",
            nullable = false)
    @NotNull
    private Instant lastModifiedAt;

    public Resident() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Resident setId(UUID id) {
        this.id = id;
        return this;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Resident setOrganization(Organization organization) {
        this.organization = organization;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Resident setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public Resident setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Resident setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public Resident setSex(String sex) {
        this.sex = sex;
        return this;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Resident setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Resident setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Resident setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public Resident setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public ResidentView toView() {
        return new ResidentView(
                id,
                firstName,
                middleName,
                lastName,
                sex,
                dateOfBirth,
                address,
                createdAt
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Resident that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(sex, that.sex) &&
               Objects.equals(dateOfBirth, that.dateOfBirth) &&
               Objects.equals(address, that.address) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                firstName,
                middleName,
                lastName,
                sex,
                dateOfBirth,
                address,
                createdAt,
                lastModifiedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("dateOfBirth", dateOfBirth)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }
}
