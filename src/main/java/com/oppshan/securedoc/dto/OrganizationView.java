package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.Organization;

import java.io.Serial;
import java.io.Serializable;

public class OrganizationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1050625973901239020L;

    private Long id;
    private Organization.Type type;
    private String name;
    private String code;
    private String address;

    public OrganizationView() {
    }

    public OrganizationView(Long id, Organization.Type type, String name, String code, String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.code = code;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization.Type getType() {
        return type;
    }

    public void setType(Organization.Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
