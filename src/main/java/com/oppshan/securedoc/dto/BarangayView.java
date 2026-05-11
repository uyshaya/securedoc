package com.oppshan.securedoc.dto;

import java.io.Serial;
import java.io.Serializable;

public class BarangayView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1050625973901239020L;

    private Long id;
    private String name;
    private String code;
    private String address;

    public BarangayView() {
    }

    public BarangayView(Long id, String name, String code, String address) {
        this.id = id;
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