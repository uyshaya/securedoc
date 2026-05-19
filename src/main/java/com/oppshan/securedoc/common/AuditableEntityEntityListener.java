package com.oppshan.securedoc.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@ApplicationScoped
public class AuditableEntityEntityListener {

    @PrePersist
    public void onPrePersist(Object object) {
        if (!(object instanceof AuditableEntity<?> auditableEntity)) {
            return;
        }

        final var now = Instant.now();
        auditableEntity.setCreatedAt(now);
        auditableEntity.setLastModifiedAt(now);
    }

    @PreUpdate
    public void onPreUpdate(Object object) {
        if (!(object instanceof AuditableEntity<?> auditableEntity)) {
            return;
        }

        auditableEntity.setLastModifiedAt(Instant.now());
    }
}
