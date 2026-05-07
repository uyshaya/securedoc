package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.model.Barangay;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Named
@ApplicationScoped
public class SystemConfigBean {

    public static final String APPLICATION_NAME = "SecureDoc";
    public static final String APPLICATION_VERSION = "1.0.0";

    // ── Cryptographic key paths ──────────────────────────────────
    // ECDSA private + public key paths used to live here,
    // injected via @ConfigProperty from application.properties:
    // TODO key paths
    // @Inject
    // @ConfigProperty(name = "securedoc.crypto.ecdsa.private-key-path")
    // String ecdsaPrivateKeyPath;

    private final List<Barangay> barangays = new CopyOnWriteArrayList<>();

    @PostConstruct
    void init() {
        // TODO: replace with DB-backed loading once the Barangay table is populated.
        barangays.add(new Barangay(1L, "Brgy. Apas", "AP-001", "Cebu City"));
        barangays.add(new Barangay(2L, "Brgy. Maligaya", "MA-002", "Caloocan City"));
        barangays.add(new Barangay(3L, "Brgy. Bagumbayan", "BA-003", "Manila"));
    }

    public List<Barangay> getBarangays() {
        return List.copyOf(barangays);
    }

    public Barangay findById(Long id) {
        if (id == null) {
            return null;
        }
        for (Barangay b : barangays) {
            if (id.equals(b.getId())) return b;
        }
        return null;
    }

    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    public String getApplicationVersion() {
        return APPLICATION_VERSION;
    }
}
