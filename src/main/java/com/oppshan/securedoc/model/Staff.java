package com.oppshan.securedoc.model;

import com.oppshan.securedoc.dto.StaffRegistrationView;
import com.oppshan.securedoc.dto.StaffView;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "staff")
public class Staff implements Serializable {

    @Serial
    private static final long serialVersionUID = 1130888729109415175L;

    public enum Role {
        ADMIN,
        STAFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = RoleConverter.class)
    @Column(columnDefinition = "ENUM('staff','admin') DEFAULT 'staff'")
    private Role role = Role.STAFF;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public Staff() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lastName.trim());
        }
        return sb.toString();
    }

    public StaffView toView() {
        StaffView view = new StaffView();
        view.setId(id);
        view.setFirstName(firstName);
        view.setMiddleName(middleName);
        view.setLastName(lastName);
        view.setFullName(getFullName());
        view.setEmail(email);
        view.setRole(role);
        view.setIsActive(isActive);
        view.setOrganizationId(organization != null ? organization.getId() : null);
        view.setLastLogin(lastLogin);
        return view;
    }

    /** Narrow projection — just enough to confirm the persisted registration. */
    public StaffRegistrationView toRegistrationView() {
        StaffRegistrationView view = new StaffRegistrationView();
        view.setId(id);
        view.setFullName(getFullName());
        view.setEmail(email);
        view.setOrganizationId(organization != null ? organization.getId() : null);
        view.setIsActive(isActive);
        return view;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Staff)) {
            return false;
        }
        return Objects.equals(id, ((Staff) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Bridges the Java {@link Role} (uppercase, JVM convention) with the
     * lowercase MySQL ENUM('staff','admin') in the staff.role column.
     */
    @Converter
    public static class RoleConverter implements AttributeConverter<Role, String> {

        @Override
        public String convertToDatabaseColumn(Role role) {
            return role == null ? null : role.name().toLowerCase();
        }

        @Override
        public Role convertToEntityAttribute(String s) {
            return s == null ? null : Role.valueOf(s.toUpperCase());
        }
    }
}
