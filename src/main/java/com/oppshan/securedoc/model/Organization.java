package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tenant root. A single deployment hosts one organization type
 * (barangay, school, city, ...) selected via the
 * {@code securedoc.org.active-type} application property; the
 * {@link Type} discriminator on each row keeps the table open
 * to additional types being added without a schema rewrite.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "organization",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_organizations_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_organizations_type",
                        columnList = "type"
                ),
                @Index(
                        name = "idx_organizations_name",
                        columnList = "name"
                )
        }
)
public class Organization
        implements AuditableEntity<Organization>, Serializable {

    @Serial
    private static final long serialVersionUID = 2723256079779038097L;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "type",
            nullable = false,
            updatable = false,
            columnDefinition = "ENUM('BARANGAY','SCHOOL','CITY') NOT NULL DEFAULT 'BARANGAY'")
    @NotNull
    private Type type = Type.BARANGAY;

    @Basic(optional = false)
    @Column(name = "name",
            nullable = false)
    @NotEmpty
    private String name;

    @Basic(optional = false)
    @Column(name = "code",
            nullable = false,
            updatable = false,
            length = 50)
    @NotEmpty
    private String code;

    @Column(name = "address",
            columnDefinition = "TEXT")
    @Nullable
    private String address;

    @Column(name = "contact_number",
            length = 20)
    @Nullable
    private String contactNumber;

    @Column(name = "email")
    @Nullable
    private String email;

    @Basic(optional = false)
    @Column(name = "active",
            nullable = false)
    private boolean active = true;

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

    public Organization() {
    }

    public Organization(UUID id, Type type, String name, String code, String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.code = code;
        this.address = address;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Organization setId(UUID id) {
        this.id = id;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Organization setType(Type type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Organization setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Organization setCode(String code) {
        this.code = code;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public Organization setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }

    @Nullable
    public String getContactNumber() {
        return contactNumber;
    }

    public Organization setContactNumber(@Nullable String contactNumber) {
        this.contactNumber = contactNumber;
        return this;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public Organization setEmail(@Nullable String email) {
        this.email = email;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public Organization setActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Organization setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public Organization setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public OrganizationView toView() {
        return new OrganizationView(id, type, name, code, address);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Organization that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               type == that.type &&
               Objects.equals(name, that.name) &&
               Objects.equals(code, that.code) &&
               Objects.equals(address, that.address) &&
               Objects.equals(contactNumber, that.contactNumber) &&
               Objects.equals(email, that.email) &&
               active == that.active &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                type,
                name,
                code,
                address,
                contactNumber,
                email,
                active,
                createdAt,
                lastModifiedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("name", name)
                .add("code", code)
                .add("address", address)
                .add("contactNumber", contactNumber)
                .add("email", email)
                .add("active", active)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }

    public enum Type {
        BARANGAY,
        SCHOOL,
        CITY
    }
}
