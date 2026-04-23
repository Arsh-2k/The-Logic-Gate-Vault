```
████████╗██╗  ██╗███████╗    ██╗      ██████╗  ██████╗ ██╗ ██████╗
╚══██╔══╝██║  ██║██╔════╝    ██║     ██╔═══██╗██╔════╝ ██║██╔════╝
   ██║   ███████║█████╗      ██║     ██║   ██║██║  ███╗██║██║
   ██║   ██╔══██║██╔══╝      ██║     ██║   ██║██║   ██║██║██║
   ██║   ██║  ██║███████╗    ███████╗╚██████╔╝╚██████╔╝██║╚██████╗
   ╚═╝   ╚═╝  ╚═╝╚══════╝    ╚══════╝ ╚═════╝  ╚═════╝ ╚═╝ ╚═════╝

 ██████╗  █████╗ ████████╗███████╗    ██╗   ██╗ █████╗ ██╗   ██╗██╗  ████████╗
██╔════╝ ██╔══██╗╚══██╔══╝██╔════╝    ██║   ██║██╔══██╗██║   ██║██║  ╚══██╔══╝
██║  ███╗███████║   ██║   █████╗      ██║   ██║███████║██║   ██║██║     ██║
██║   ██║██╔══██║   ██║   ██╔══╝      ╚██╗ ██╔╝██╔══██║██║   ██║██║     ██║
╚██████╔╝██║  ██║   ██║   ███████╗     ╚████╔╝ ██║  ██║╚██████╔╝███████╗██║
 ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝      ╚═══╝  ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝

  Team V8 Logic Systems  |  Arshpreet Singh  |  Roll No: S25CSEU0980
  Course: 2025CSET152 — OOP in Java  |  Batch 33  |  Bennett University
  GitHub: github.com/Arsh-2k/The-Logic-Gate-Vault
```

---

## Overview

**The Logic-Gate Vault** is a Java 21 Swing desktop application for secure file encryption. It implements a dual-cipher engine (AES-256 and XOR), role-based access control (RBAC), cryptographic Key Escrow, a thread-safe persistent audit trail, and an optional Google Drive sync module — structured around a strict four-process Data Flow Diagram (DFD) architecture.
---
## System Specifications & Limits

Supported Formats: Universal byte-level support. Works with ALL file types (e.g., .txt, .pdf, .docx, .mp4, .zip, .exe, .jpg).

File Size Limit: ~256 MB per file (processed safely in-memory). Files exceeding this limit will trigger a graceful "Out of Memory" fallback warning rather than crashing the application.

Concurrency: Fully thread-safe background processing using SwingWorker, keeping the UI responsive during heavy batch encryptions.

## Quick Start

### Prerequisites
- JDK 21 installed (`java --version` should show 21.x)
- All files in the structure shown below

### 1 — Compile
Open a terminal in the project root folder:

```bash
javac -encoding UTF-8 -d out src\*.java
```

*(macOS/Linux: use forward slashes — `javac -encoding UTF-8 -d out src/*.java`)*

### 2 — Run
```bash
java -cp out MainApp
```

### Default Login Credentials

| Role  | Username | Password   |
|-------|----------|------------|
| Admin | `admin`  | `Admin@123`|
| User  | `user`   | `User@123` |

---

## Directory Structure

```
The-Logic-Gate-Vault/
│
├── src/                        ← All Java source files
│   ├── MainApp.java            ← Entry point + Swing GUI (M3)
│   ├── EncryptionEngine.java   ← Abstract base class (Abstraction)
│   ├── XOREngine.java          ← M1: XOR cipher (Inheritance)
│   ├── AESEngine.java          ← M2: AES-256/CBC + Key Escrow
│   ├── BatchProcessor.java     ← M2: SwingWorker batch mode
│   ├── FileHandler.java        ← Atomic file read/write
│   ├── ActivityLogger.java     ← M3: P3 Singleton audit logger → D3
│   ├── CloudSyncManager.java   ← M4: P4 Google Drive sync → D4
│   ├── AuthManager.java        ← M5: P1 RBAC + PBKDF2 → D1
│   ├── EventDispatcher.java    ← Interface decoupling GUI from logic
│   └── FileOrchestrator.java   ← Central hub (Polymorphism)
│
├── out/                        ← Compiled .class files (git-ignored)
│
├── logs/                       ← D3: activity.log written here
│   └── activity.log            ← Timestamped audit trail (git-ignored)
│
├── data/                       ← Runtime data stores
│   ├── users.dat               ← D1: User credentials (git-ignored)
│   ├── admin.cfg               ← D2: Admin master key config (git-ignored)
│   └── .vault                  ← D4: Google Drive FileId mappings (git-ignored)
│
├── .gitignore
├── LICENSE
└── README.md
```

---

## DFD Architecture (P1 – P4)

```
┌─────────────────────────────────────┐
   USER                 │      LOGIC-GATE VAULT SYSTEM        │
  (File +   ──────────▶ │                                     │ ──▶  Encrypted .enc
  Password)             │  ┌────┐  ┌────┐  ┌────┐  ┌────┐   │ ──▶  Activity Log
                        │  │ P1 │  │ P2 │  │ P3 │  │ P4 │   │ ──▶  Google Drive
                        │  │Auth│  │Enc/│  │Audit│  │Cloud│  │
                        │  │    │  │Dec │  │ Log │  │Sync │  │
                        │  └────┘  └────┘  └─────┘  └────┘   │
                        └─────────────────────────────────────┘
                              │        │        │        │
                             D1       D2       D3       D4
                          users.dat admin.cfg activity .vault
                                              .log
```

### P1 — Authentication (AuthManager.java → D1 data/users.dat)
Verifies credentials using PBKDF2WithHmacSHA256 (65,536 iterations, 16-byte salt). Implements two RBAC roles: ADMIN and USER. Accounts are safely serialized. Sessions are strictly enforced with a 15-minute inactivity timeout.

### P2 — Encrypt/Decrypt (`AESEngine.java`, `XOREngine.java`, `FileOrchestrator.java` → D2 `data/admin.cfg`)
Dual-algorithm engine supporting:
- **AES-256/CBC/PKCS5Padding** — production-grade, 96-byte header with IV + Key Escrow + SHA-256 checksum
- **XOR** — O(n) bitwise cipher, pedagogical demo only, with external SHA-256 integrity tag

Key Escrow: the session key is AES-wrapped with the admin's master key and stored at header bytes 16-63. Admin can recover any file without the user's password.

### P3 — Audit Log (`ActivityLogger.java` → D3 `logs/activity.log`)
Thread-safe Singleton (double-checked locking). Appends timestamped entries with username, operation type, algorithm, file size, and duration. Persists across JVM restarts.

### P4 — Cloud Sync (`CloudSyncManager.java` → D4 `data/.vault`)
Functional UI mock implementation of a zero-knowledge Google Drive upload. Only .enc files may be uploaded (plaintext is rejected). FileId mappings are persisted to data/.vault as a Java Properties file. (Phase 2 will integrate the Google Drive REST API v3).

---

## Encrypted File Format

### AES-256 `.enc` (96-byte header)
```
Bytes  0-15 : IV — random 16 bytes (SecureRandom, fresh per encryption)
Bytes 16-63 : Wrapped session key (Key Escrow — AES/CBC encrypted, 48 bytes)
Bytes 64-95 : SHA-256 checksum of original plaintext (32 bytes)
Bytes 96+   : AES/CBC/PKCS5 ciphertext
```

### XOR `.enc`
```
Bytes  0-31 : SHA-256 checksum of original plaintext (32 bytes)
Bytes 32+   : XOR ciphertext
```

---

## OOP Pillars — Where Each Is Demonstrated

| Pillar | Class | How |
|--------|-------|-----|
| **Abstraction** | `EncryptionEngine` | Abstract class defines `encrypt()`/`decrypt()` without implementation |
| **Inheritance** | `XOREngine`, `AESEngine` | Both `extend EncryptionEngine`, call `super("XOR")` / `super("AES-256")` |
| **Polymorphism** | `FileOrchestrator` | `engine.encrypt()` dispatches to XOR or AES at runtime via a single `EncryptionEngine` reference |
| **Encapsulation** | `AuthManager` | `UserCredential` is a private inner class; only `authenticate()` is public |

---

## How to Use

1. **Login** — Enter credentials in the login dialog. Use the new "Show" toggle to verify your password.
2. **Encrypt** — Browse → select algorithm → enter strong password → click ENCRYPT. The original file is deleted and replaced with a .enc file.
3. **Decrypt** — Browse a .enc file → select the SAME algorithm → enter the SAME password → click DECRYPT.
4. **Batch** — Click 'Add Files' → enter password → 'Batch Encrypt'. Runs safely in the background while the UI remains live.
5. **Key Escrow** Select a .enc file → 'Sync to Drive' (simulated demo). A WIP transparency notice will appear.
6. **Clear Log** (Admin only) — Access the live "Monitoring Console", use 'Key Escrow Recovery' to decrypt without user passwords, or 'Clear Activity Log'.

**Password strength rules:** A STRONG password requires 8+ characters, at least one special character (@, #, $, !, etc.), AND at least one digit (0-9).

---

*Bennett University | SCSET | 2025CSET152 | Batch 33 | S25CSEU0980*
