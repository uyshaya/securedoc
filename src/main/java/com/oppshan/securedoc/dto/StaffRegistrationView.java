package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Narrow projection returned by {@code AdminAuthService.createStaff}
 * after a self-service registration succeeds. Confirms the persisted
 * row without exposing fields the registration flow doesn't need
 * (lastLogin, role, etc.). Built by {@code Staff.toRegistrationView()}.
 */
public class StaffRegistrationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 5199120916097796464L;

    private UUID id;

    private String fullName;

    private String email;

    private UUID organizationId;

    private Boolean active;

    public StaffRegistrationView() {
    }

    public UUID getId() {
        return id;
    }

    public StaffRegistrationView setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public StaffRegistrationView setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public StaffRegistrationView setEmail(String email) {
        this.email = email;
        return this;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public StaffRegistrationView setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public Boolean isActive() {
        return active;
    }

    public StaffRegistrationView setActive(Boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final StaffRegistrationView that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(fullName, that.fullName) &&
               Objects.equals(email, that.email) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fullName, email, organizationId, active);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("fullName", fullName)
                .add("email", email)
                .add("organizationId", organizationId)
                .add("active", active)
                .toString();
    }
}
