package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.model.Staff;
import jakarta.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * View-facing projection of a {@link com.oppshan.securedoc.model.Staff} row.
 * Mutable POJO so JSF {@code <h:selectOneMenu value="#{s.role}">} can
 * write the new role back before the bean listener delegates the change
 * to the service. Entities never cross the service boundary.
 */
public class StaffView implements Serializable {

    @Serial
    private static final long serialVersionUID = 976875839521764727L;

    private UUID id;

    private String firstName;

    @Nullable
    private String middleName;

    private String lastName;

    private String fullName;

    private String email;

    private Staff.Role role;

    private Boolean active;

    private UUID organizationId;

    @Nullable
    private Instant lastLogin;

    public StaffView() {
    }

    public UUID getId() {
        return id;
    }

    public StaffView setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public StaffView setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public StaffView setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public StaffView setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public StaffView setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public StaffView setEmail(String email) {
        this.email = email;
        return this;
    }

    public Staff.Role getRole() {
        return role;
    }

    public StaffView setRole(Staff.Role role) {
        this.role = role;
        return this;
    }

    public Boolean isActive() {
        return active;
    }

    public StaffView setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public StaffView setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    @Nullable
    public Instant getLastLogin() {
        return lastLogin;
    }

    public StaffView setLastLogin(@Nullable Instant lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final StaffView that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(fullName, that.fullName) &&
               Objects.equals(email, that.email) &&
               role == that.role &&
               Objects.equals(active, that.active) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(lastLogin, that.lastLogin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                firstName,
                middleName,
                lastName,
                fullName,
                email,
                role,
                active,
                organizationId,
                lastLogin
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("firstName", firstName)
                .add("middleName", middleName)
                .add("lastName", lastName)
                .add("fullName", fullName)
                .add("email", email)
                .add("role", role)
                .add("active", active)
                .add("organizationId", organizationId)
                .add("lastLogin", lastLogin)
                .toString();
    }
}
