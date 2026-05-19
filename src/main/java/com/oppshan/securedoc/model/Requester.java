package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Personal info snapshot for one resident submission. Not unique by email --
 * the same person submitting twice produces two rows, so each {@link Request}
 * owns its own requester row and edits to a later submission don't mutate
 * the record of an earlier one.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "requester",
        indexes = {
                @Index(
                        name = "idx_requester_email",
                        columnList = "email"
                )
        }
)
public class Requester
        implements AuditableEntity<Requester>, Serializable {

    @Serial
    private static final long serialVersionUID = 6184729103847562918L;

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
    @Column(name = "email",
            nullable = false,
            length = 255)
    @NotEmpty
    private String email;

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

    @Column(name = "contact_number",
            length = 20)
    @Nullable
    private String contactNumber;

    @Column(name = "id_type",
            length = 50)
    @Nullable
    private String idType;

    @Column(name = "id_image_path",
            length = 500)
    @Nullable
    private String idImagePath;

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

    public Requester() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Requester setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Requester setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public Requester setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Requester setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Requester setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public Requester setSex(String sex) {
        this.sex = sex;
        return this;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Requester setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    @Nullable
    public String getContactNumber() {
        return contactNumber;
    }

    public Requester setContactNumber(@Nullable String contactNumber) {
        this.contactNumber = contactNumber;
        return this;
    }

    @Nullable
    public String getIdType() {
        return idType;
    }

    public Requester setIdType(@Nullable String idType) {
        this.idType = idType;
        return this;
    }

    @Nullable
    public String getIdImagePath() {
        return idImagePath;
    }

    public Requester setIdImagePath(@Nullable String idImagePath) {
        this.idImagePath = idImagePath;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Requester setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public Requester setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Requester that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email) &&
               Objects.equals(sex, that.sex) &&
               Objects.equals(dateOfBirth, that.dateOfBirth) &&
               Objects.equals(contactNumber, that.contactNumber) &&
               Objects.equals(idType, that.idType) &&
               Objects.equals(idImagePath, that.idImagePath) &&
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
                email,
                sex,
                dateOfBirth,
                contactNumber,
                idType,
                idImagePath,
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
                .add("email", email)
                .add("dateOfBirth", dateOfBirth)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }
}
