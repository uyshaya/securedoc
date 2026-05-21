package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Form payload submitted from /user/request.xhtml. Bundles every
 * field the resident filled in across the verify + details scenes
 * so {@code RequestService.submitRequest} takes one argument
 * instead of a long positional list. Built by {@code RequestBean}
 * right before the service call.
 */
public class RequestCreate implements Serializable {

    @Serial
    private static final long serialVersionUID = 9183746102837465019L;

    @NotNull
    private UUID organizationId;

    @NotNull
    private UUID templateId;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Nullable
    @Size(max = 100)
    private String middleName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotNull
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(max = 1)
    private String sex;

    @Nullable
    @Size(max = 20)
    private String contactNumber;

    @Nullable
    @Size(max = 50)
    private String idType;

    @NotNull
    private byte[] idImageData;

    @Nullable
    @Size(max = 50)
    private String purpose;

    @Nullable
    @Size(max = 255)
    private String otherPurpose;

    public RequestCreate() {
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public RequestCreate setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public RequestCreate setTemplateId(UUID templateId) {
        this.templateId = templateId;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public RequestCreate setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public RequestCreate setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public RequestCreate setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public RequestCreate setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public RequestCreate setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public RequestCreate setSex(String sex) {
        this.sex = sex;
        return this;
    }

    @Nullable
    public String getContactNumber() {
        return contactNumber;
    }

    public RequestCreate setContactNumber(@Nullable String contactNumber) {
        this.contactNumber = contactNumber;
        return this;
    }

    @Nullable
    public String getIdType() {
        return idType;
    }

    public RequestCreate setIdType(@Nullable String idType) {
        this.idType = idType;
        return this;
    }

    public byte[] getIdImageData() {
        return idImageData;
    }

    public RequestCreate setIdImageData(byte[] idImageData) {
        this.idImageData = idImageData;
        return this;
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public RequestCreate setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
        return this;
    }

    @Nullable
    public String getOtherPurpose() {
        return otherPurpose;
    }

    public RequestCreate setOtherPurpose(@Nullable String otherPurpose) {
        this.otherPurpose = otherPurpose;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final RequestCreate that)) {
            return false;
        }

        return Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(templateId, that.templateId) &&
               Objects.equals(email, that.email) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(middleName, that.middleName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(dateOfBirth, that.dateOfBirth) &&
               Objects.equals(sex, that.sex) &&
               Objects.equals(contactNumber, that.contactNumber) &&
               Objects.equals(idType, that.idType) &&
               Objects.equals(purpose, that.purpose) &&
               Objects.equals(otherPurpose, that.otherPurpose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                organizationId,
                templateId,
                email,
                firstName,
                middleName,
                lastName,
                dateOfBirth,
                sex,
                contactNumber,
                idType,
                purpose,
                otherPurpose
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("organizationId", organizationId)
                .add("templateId", templateId)
                .add("email", email)
                .add("firstName", firstName)
                .add("middleName", middleName)
                .add("lastName", lastName)
                .add("dateOfBirth", dateOfBirth)
                .add("sex", sex)
                .add("contactNumber", contactNumber)
                .add("idType", idType)
                .add("purpose", purpose)
                .add("otherPurpose", otherPurpose)
                .toString();
    }
}
