package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Form payload submitted from /admin/register.xhtml. Bundles the
 * fields that {@code AdminAuthService.createStaff} needs so the
 * service signature doesn't grow a long positional argument list.
 * Bean-side fields like {@code confirmPassword} stay in the bean --
 * they never reach the service.
 */
public class StaffRegistrationCreate implements Serializable {

    @Serial
    private static final long serialVersionUID = 8892600475009793842L;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @NotNull
    private UUID organizationId;

    public StaffRegistrationCreate() {
    }

    public String getFirstName() {
        return firstName;
    }

    public StaffRegistrationCreate setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public StaffRegistrationCreate setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public StaffRegistrationCreate setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public StaffRegistrationCreate setPassword(String password) {
        this.password = password;
        return this;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public StaffRegistrationCreate setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final StaffRegistrationCreate that)) {
            return false;
        }

        // Password intentionally excluded -- we don't want it in equality semantics
        // (it's plaintext until the service hashes it; equals should be by identity here).
        return Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email) &&
               Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, organizationId);
    }

    @Override
    public String toString() {
        // password deliberately omitted -- never log plaintext credentials.
        return MoreObjects.toStringHelper(this)
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("email", email)
                .add("organizationId", organizationId)
                .toString();
    }
}
