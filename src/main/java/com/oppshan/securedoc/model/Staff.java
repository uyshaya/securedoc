package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import com.oppshan.securedoc.dto.StaffRegistrationView;
import com.oppshan.securedoc.dto.StaffView;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "staff",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_staff_organization_email",
                        columnNames = {"organization_id", "email"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_staff_organization_id",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_staff_email",
                        columnList = "email"
                )
        }
)
public class Staff
        implements AuditableEntity<Staff>, Serializable {

    @Serial
    private static final long serialVersionUID = 1130888729109415175L;

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = Organization.class
    )
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
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
    @Column(name = "email",
            nullable = false,
            length = 255)
    @NotEmpty
    private String email;

    @Basic(optional = false)
    @Column(name = "password_hash",
            nullable = false,
            length = 255)
    @NotEmpty
    private String passwordHash;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "role",
            nullable = false,
            columnDefinition = "ENUM('STAFF','ADMIN') NOT NULL DEFAULT 'STAFF'")
    @NotNull
    private Role role = Role.STAFF;

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

    @Column(name = "last_login")
    @Nullable
    private Instant lastLogin;

    public Staff() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Staff setId(UUID id) {
        this.id = id;
        return this;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Staff setOrganization(Organization organization) {
        this.organization = organization;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Staff setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public Staff setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Staff setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Staff setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Staff setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public Staff setRole(Role role) {
        this.role = role;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public Staff setActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Staff setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public Staff setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    @Nullable
    public Instant getLastLogin() {
        return lastLogin;
    }

    public Staff setLastLogin(@Nullable Instant lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    public String getFullName() {
        final var fullName = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName.trim());
        }

        if (middleName != null && !middleName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(' ');
            }

            fullName.append(middleName.trim());
        }

        if (lastName != null && !lastName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(' ');
            }

            fullName.append(lastName.trim());
        }

        return fullName.toString();
    }

    public StaffView toView() {
        return new StaffView()
                .setId(id)
                .setFirstName(firstName)
                .setMiddleName(middleName)
                .setLastName(lastName)
                .setFullName(getFullName())
                .setEmail(email)
                .setRole(role)
                .setActive(active)
                .setOrganizationId(organization != null ? organization.getId() : null)
                .setLastLogin(lastLogin);
    }

    public StaffRegistrationView toRegistrationView() {
        return new StaffRegistrationView()
                .setId(id)
                .setFullName(getFullName())
                .setEmail(email)
                .setOrganizationId(organization != null ? organization.getId() : null)
                .setActive(active);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Staff that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(organization, that.organization) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email) &&
               role == that.role &&
               active == that.active &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt) &&
               Objects.equals(lastLogin, that.lastLogin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                organization,
                firstName,
                middleName,
                lastName,
                email,
                role,
                active,
                createdAt,
                lastModifiedAt,
                lastLogin
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("organization", organization)
                .add("firstName", firstName)
                .add("middleName", middleName)
                .add("lastName", lastName)
                .add("email", email)
                .add("role", role)
                .add("active", active)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .add("lastLogin", lastLogin)
                .toString();
    }

    public enum Role {
        ADMIN("Admin"),
        STAFF("Staff");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
