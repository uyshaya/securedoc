# SecureDoc
### A Cryptographic Digital Document Issuance and Verification System

---

## Overview

**SecureDoc** is a web-based system designed to modernize and secure the document issuance and verification processes of barangay offices in the Philippines. It addresses the widespread reliance on paper-based workflows that are vulnerable to forgery, loss, and inefficiency — replacing them with a unified digital platform backed by cryptographic security.

The system's core innovation is a **tamper-evident verification portal** that allows any third party (employers, government agencies, institutions) to authenticate a barangay-issued document's integrity and confirm the legitimacy of the issuing entity — **without requiring user registration**.

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
| **Backend** | Java, Jakarta EE 10 |
| **Frontend Framework** | JSF (JavaServer Faces) + PrimeFaces 13 |
| **Database** | MySQL |
| **Security** | SHA-256, ECDSA P-256 Asymmetric Key Signing, AES-256 |
| **Build / Server** | Maven, Apache Tomcat / GlassFish |
| **Fonts** | DM Serif Display, DM Sans |

---

## Project Structure

```
SecureDoc/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/SecureDoc/
│       │       ├── beans/
│       │       │   ├── AuthBean.java          # Login, OTP, session management
│       │       │   ├── RequestBean.java        # Document request lifecycle
│       │       │   ├── TemplateBean.java       # Document template management
│       │       │   ├── ReportBean.java         # Report generation
│       │       │   ├── AuditBean.java          # Audit log viewing
│       │       │   └── ResidentBean.java       # Resident-facing request flow
│       │       ├── models/
│       │       │   ├── Staff.java
│       │       │   ├── DocumentRequest.java
│       │       │   ├── DocumentTemplate.java
│       │       │   └── AuditLog.java
│       │       ├── services/
│       │       │   ├── CryptoService.java      # SHA-256, ECDSA P-256, AES operations
│       │       │   ├── QRCodeService.java      # QR code generation
│       │       │   └── VerificationService.java # Tamper-evident verification logic
│       │       └── util/
│       │           ├── DBUtil.java             # Database connection pooling
│       │           └── MailUtil.java           # OTP email delivery
│       └── webapp/
│           ├── admin/
│           │   ├── dashboard.xhtml
│           │   ├── requests.xhtml
│           │   ├── templates.xhtml
│           │   ├── reports.xhtml
│           │   └── audit.xhtml
│           ├── user/
│           │   └── request.xhtml
│           ├── verify/
│           │   └── index.xhtml                # Zero-registration verifier portal
│           ├── login.xhtml
│           └── WEB-INF/
│               ├── web.xml
│               ├── faces-config.xml
│               └── templates/
│                   └── layout.xhtml
├── database/
│   └── SecureDoc_schema.sql
├── docs/
│   └── conceptual_framework.md
└── README.md
```

---

## Database Schema (Core Tables)

```sql
staff                  -- Barangay staff accounts
staff_otps             -- OTP records for two-factor login
document_requests      -- Resident document requests
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
