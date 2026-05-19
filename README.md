# SecureDoc
### A Cryptographic Digital Document Issuance and Verification System

---

## Overview

**SecureDoc** is a web-based system designed to modernize and secure the document issuance and verification processes of organizations (initially Philippine barangay offices; extensible to schools, cities, and other tenant types). It addresses the widespread reliance on paper-based workflows that are vulnerable to forgery, loss, and inefficiency — replacing them with a unified digital platform backed by cryptographic security.

The system's core innovation is a **tamper-evident verification portal** that allows any third party (employers, government agencies, institutions) to authenticate an organization-issued document's integrity and confirm the legitimacy of the issuing entity — **without requiring user registration**.

---

## The Problem

Barangay offices in the Philippines issue several critical documents daily — clearances, certificates of residency, indigency certifications, and more. The current state of these processes is characterized by:

1. **Manual, paper-based issuance** with no digital record-keeping infrastructure.
2. **No tamper-detection mechanism** — documents can be forged or altered without any means of detection.
3. **No real-time status tracking** — residents cannot check whether their request has been processed.
4. **No zero-registration verification** — mandatory registration requirements
   create friction that discourages residents from adopting digital systems;
   third parties cannot verify document authenticity without creating an account
   or contacting the barangay directly.
---

## Research Gap

> *"No existing study integrates frictionless document issuance, real-time status verification, tamper-evident authenticity checking, and zero-registration access within a single unified system."*

Existing solutions — including barangay management systems and the eBarangay mobile app — partially address these concerns but fall short by requiring mandatory registration for verification or by omitting a verification portal entirely.

---

## Proposed Solution

SecureDoc is a three-portal web system:

| Portal | Users | Purpose |
|---|---|---|
| **Admin Console** | Authorized barangay staff | Manage document requests, issue certificates, manage templates, generate reports, view audit logs |
| **Resident Portal** | Barangay residents | Submit document requests, upload supporting files, track request status in real time |
| **Verifier Portal** | Third parties (employers, agencies, institutions) | Authenticate issued documents via QR code or reference number — no registration required |

---

## Cryptographic Security Layer

Every issued document is protected by a multi-layered cryptographic pipeline:

```
Document Data
     │
     ▼
SHA-256 Hash ──────────────────── stored in DB
     │
     ▼
ECDSA P-256 Private Key Signing ────────── digital signature embedded in document
     │
     ▼
AES-256 Encryption ─────────────── sensitive fields encrypted at rest
     │
     ▼
QR Code / Reference Number ─────── issued to resident for sharing
```

During verification, the portal:
1. Recomputes the SHA-256 hash of the document data.
2. Validates the ECDSA P-256 digital signature using the barangay's public key.
3. Confirms the issuing entity is a registered, legitimate barangay office.

**Verification Outcomes:**
- ✅ **Document Verified** — hash integrity confirmed + issuing entity authenticated.
- ⚠️ **Tamper Detected** — hash mismatch; document content has been altered.
- ❌ **Unverified Source** — document hash valid but issuing entity cannot be confirmed.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Runtime** | Quarkus 3.35.1 on Oracle GraalVM 25 |
| **Servlet container** | Undertow (embedded), with a virtual-thread-per-task executor wired via SPI |
| **REST** | RESTEasy Classic (`quarkus-resteasy`, blocking) -- needed for `@SessionScoped` JSF beans |
| **Frontend Framework** | JSF (MyFaces) + PrimeFaces 4.15.15 via `io.quarkiverse.primefaces` |
| **Persistence** | Jakarta Data 1.0 over Hibernate ORM 7.3 (CrudRepository + StatefulWriteRepository) |
| **Schema migrations** | Flyway, `db/migration/mysql/V{N}__*.sql` |
| **Database** | MySQL 8 (DevServices container in dev/test; `QUARKUS_DATASOURCE_*` env vars in prod) |
| **i18n** | `messages.properties` via JSF `<resource-bundle>` (app-wide) + `I18n` CDI service for Java code |
| **Security** | BCrypt (BouncyCastle direct API) for passwords; SHA-256 / ECDSA P-256 / AES-256 for documents (designed, not yet implemented) |
| **Build** | Maven (wrapper committed), Java 25 source/target |
| **Native** | GraalVM native-image profiles `-Pnative` (debug) and `-Pnative-release` (optimized) |
| **CI/CD** | GitHub Actions: `maven.yml` (JaCoCo coverage), `deploy.yml` (AWS OIDC -> S3 -> SSM Run Command) |
| **Fonts** | DM Serif Display, DM Sans |

---

## Project Structure

```
securedoc/
+- src/
|  +- main/
|  |  +- java/com/oppshan/securedoc/
|  |  |  +- common/      # AuditableEntity, AuditableEntityEntityListener,
|  |  |  |               # StatefulWriteRepository, VirtualThreadServletExtension, I18n
|  |  |  +- bean/        # JSF @Named managed beans (AdminAuthBean, RequestBean,
|  |  |  |               # OrganizationBean, StaffManagementBean, ...)
|  |  |  +- service/     # @ApplicationScoped business logic; transactional boundary
|  |  |  +- repository/  # Jakarta Data @Repository interfaces (CrudRepository<T, UUID>
|  |  |  |               # + StatefulWriteRepository<T>); impls generated at compile time
|  |  |  +- model/       # JPA @Entity classes (Organization, Staff, StaffOtp, DocumentTemplate)
|  |  |  +- dto/         # Read-only projections across the bean/service boundary
|  |  |  +- web/         # AdminAuthFilter, OrganizationViewConverter
|  |  +- resources/
|  |     +- application.properties
|  |     +- messages.properties             # i18n strings (English baseline)
|  |     +- META-INF/
|  |     |  +- web.xml
|  |     |  +- faces-config.xml             # Application-wide <resource-bundle> binding
|  |     |  +- services/
|  |     |  |  +- io.undertow.servlet.ServletExtension  # Virtual-thread executor SPI
|  |     |  +- resources/
|  |     |     +- index.xhtml               # Portal landing page
|  |     |     +- admin/                    # login, register, dashboard, requests,
|  |     |     |                            # templates, audit, staff/staff-management
|  |     |     +- user/request.xhtml        # Resident multi-scene request flow
|  |     |     +- verifier/verify.xhtml     # Zero-registration verification portal
|  |     |     +- WEB-INF/templates/        # admin-layout, admin-auth-layout,
|  |     |     |                            # resident-layout, verifier-layout
|  |     |     +- css/, js/                 # global.css, admin.css, request.css,
|  |     |                                  # verifier.css, request.js, local-time.js
|  |     +- db/
|  |        +- setup.sql                    # DB + dev user provisioning (run once)
|  |        +- migration/mysql/             # Flyway migrations (V1__init.sql, V2__...)
|  +- test/java/com/oppshan/securedoc/
|     +- SchemaSmokeTest.java               # @QuarkusTest -- MySQL container + Flyway + JPA round-trip
+- .github/workflows/                       # maven.yml, deploy.yml
+- docs/
│   └── conceptual_framework.md
└── README.md
```

---

## Database Schema (Core Tables)

```sql
organizations          -- Tenant root (barangay/school/city) — discriminated by `type`
staff                  -- Organization staff accounts (FK organization_id)
staff_otps             -- OTP records for two-factor login
org_certificates       -- Per-organization signing certificate metadata
document_requests      -- Resident document requests (FK organization_id)
document_templates     -- Issuable document types and layouts
issued_documents       -- Finalized, cryptographically signed documents
verification_log       -- Audit trail of all third-party verification attempts
audit_log              -- System-wide activity log
```

---

## SDG Alignment

| Goal | Alignment |
|---|---|
| **SDG 9** — Industry, Innovation & Infrastructure | Modernizes public service infrastructure through digital innovation |
| **SDG 11** — Sustainable Cities & Communities | Improves access to civic services, reducing barriers for marginalized residents |

---

## Key Concepts

- **Tamper-evident** — the system can *detect* unauthorized modifications to issued documents; it does not prevent physical tampering outright, but any alteration is detectable during verification.
- **Zero-registration access** — third-party verifiers can authenticate documents with only a reference number or QR code, with no account creation required.
- **Frictionless issuance** — residents submit requests digitally and track status in real time without visiting the barangay office for follow-ups.

---

## Academic Context

This system is developed as a research project examining the intersection of **e-governance**, **document security**, and **public service accessibility** in the Philippine local government context. It contributes to the body of knowledge by demonstrating a unified architecture that resolves the four identified gaps simultaneously — something no prior study has achieved.

---

*SecureDoc — Issued with integrity. Verified with certainty.*
