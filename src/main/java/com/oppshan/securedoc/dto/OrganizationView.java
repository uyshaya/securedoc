package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.model.Organization;
import jakarta.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class OrganizationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1050625973901239020L;

    private UUID id;

    private Organization.Type type;

    private String name;

    private String code;

    @Nullable
    private String address;

    public OrganizationView() {
    }

    public OrganizationView(UUID id, Organization.Type type, String name, String code, @Nullable String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.code = code;
        this.address = address;
    }

    public UUID getId() {
        return id;
    }

    public OrganizationView setId(UUID id) {
        this.id = id;
        return this;
    }

    public Organization.Type getType() {
        return type;
    }

    public OrganizationView setType(Organization.Type type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrganizationView setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public OrganizationView setCode(String code) {
        this.code = code;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public OrganizationView setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final OrganizationView that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               type == that.type &&
               Objects.equals(name, that.name) &&
               Objects.equals(code, that.code) &&
               Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name, code, address);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("name", name)
                .add("code", code)
                .add("address", address)
                .toString();
    }
}
