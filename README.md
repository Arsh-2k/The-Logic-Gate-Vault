# 🔐 The Logic-Gate Vault

![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen)

**A Java desktop application that encrypts and decrypts files using XOR and AES-256, built with a Swing GUI, concurrent batch processing, and a timestamped audit log.**

> Academic mini-project | OOP in Java | Bennett University | 2025–26  
> Team V8 Logic Systems · Arshpreet Singh · S25CSEU0980

---

## 📋 Overview

The Logic-Gate Vault is a multi-module file encryption system developed as part of the B.Tech CSE curriculum at Bennett University. It demonstrates progression from fundamental bitwise logic (XOR) to industry-standard cryptography (AES-256), wrapped in a dark-themed Swing GUI with real-time logging and concurrent batch mode.

The project is structured across four modules:

| Module | Description | Status |
|--------|-------------|--------|
| M1 | XOR Encryption Engine | ✅ Complete |
| M2 | AES-256 + Batch Processing | ✅ Complete |
| M3 | Swing GUI + Activity Logger | 🔄 In Progress |
| M4 | Firebase Cloud Sync | 📅 Planned |

---

## ✨ Features

- **Dual-algorithm encryption** — XOR (O(n), self-inverse) and AES-256/CBC/PKCS5
- **Swing GUI dashboard** — dark-themed JFrame with file browser, algorithm selector, password field, and live log viewer
- **Batch mode** — encrypt multiple files concurrently using a thread pool sized to available CPU cores
- **Timestamped audit log** — every encrypt/decrypt action is written to `logs/activity.log` with filename, algorithm, file size, and elapsed time
- **Supports all file types** — `.txt`, `.docx`, `.xlsx`, `.pdf`, `.png`, `.jpg`, `.zip`, and any binary format
- **Session persistence** — activity log reloads from disk on startup so history is never lost

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 (JDK 21 LTS) |
| GUI | `javax.swing` — JFrame, JPanel, JComboBox, JPasswordField, JProgressBar |
| Cryptography | `javax.crypto` — AES/CBC/PKCS5Padding, SecureRandom, SHA-256 |
| Concurrency | `java.util.concurrent` — ExecutorService, Future, Callable |
| File I/O | `java.nio.file` — Files.readAllBytes, Files.write |
| Logging | `java.time.LocalDateTime`, BufferedWriter |
| Version Control | Git + GitHub |

---

## 📁 Project Structure

```
The-Logic-Gate-Vault/
├── src/
│   ├── MainApp.java            # Entry point — launches GUI on EDT
│   ├── EncryptionEngine.java   # XOR + AES-256 logic, SHA-256 key derivation
│   ├── FileHandler.java        # File I/O, single-file and batch encrypt/decrypt
│   ├── ActivityLogger.java     # Timestamped audit log writer
│   └── GUIDashboard.java       # Swing window — all UI components and event handlers
├── out/                        # Compiled .class files (git-ignored)
├── logs/
│   └── activity.log            # Auto-generated audit log (git-ignored)
├── .gitignore
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

- JDK 21 or higher — [Download](https://adoptium.net/)
- Windows, macOS, or Linux
- VS Code with Java Extension Pack (recommended) — or any terminal

### Compile & Run

```bash
# 1. Clone the repository
git clone https://github.com/Arsh-2k/The-Logic-Gate-Vault.git
cd The-Logic-Gate-Vault

# 2. Create output directories
mkdir out
mkdir logs

# 3. Compile all source files
javac -d out src/*.java

# 4. Run the application
java -cp out MainApp
```

---

## 🖥️ Usage

### Single File Encryption

1. Click **Browse** and select any file
2. Choose algorithm — **XOR** or **AES-256**
3. Enter a password
4. Click **ENCRYPT** — output saved as `<filename>.enc` in the same directory
5. To decrypt, select the `.enc` file, enter the same password, click **DECRYPT**

### Batch Encryption

1. Click **Add Files** to queue multiple files
2. Choose algorithm and enter password
3. Click **Batch Encrypt** — files are processed concurrently
4. Progress bar and status bar update in real time

### Activity Log

The right panel shows `logs/activity.log` — every action is recorded with:

```
2026-03-13 10:22:01  [ENCRYPT]
  File:  notes.txt     Algo: XOR
  Size:  2,048 bytes   Time: 3ms
------------------------------------------------
```

---

## 🔒 Encryption Details

### XOR (Module 1)

```java
result[i] = (byte)(data[i] ^ key[i % key.length]);
```

- Each byte of the file is XORed with the corresponding key byte
- Key cycles over the full file length using modulo
- Self-inverse — the same operation encrypts and decrypts
- Time complexity: O(n), single pass

### AES-256 (Module 2)

```java
// Key derivation
MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"))
// → always 32 bytes regardless of password length

// Encryption
Cipher.getInstance("AES/CBC/PKCS5Padding");
new SecureRandom().nextBytes(iv);  // fresh 16-byte IV every time
// Output format: [IV (16 bytes)][ciphertext]
```

- Password is hashed with SHA-256 to produce a 32-byte (256-bit) key
- Random IV generated per encryption using `SecureRandom`
- IV is prepended to the output file for use during decryption
- Mode: CBC (Cipher Block Chaining) — each block depends on the previous

---

## 📐 Architecture

### Class Responsibilities

```
MainApp            → Entry point, launches GUIDashboard on the Event Dispatch Thread
EncryptionEngine   → Owns algorithm + key; exposes encrypt(byte[]) and decrypt(byte[])
FileHandler        → Reads/writes files as byte arrays; coordinates single and batch ops
ActivityLogger     → Thread-safe log writer; persists to disk, reloads on startup
GUIDashboard       → All Swing components; uses SwingWorker for non-blocking operations
```

### Key Design Decisions

- **SwingWorker** used for both single-file and batch operations — encryption never runs on the EDT, so the UI stays responsive
- **synchronized** on all ActivityLogger write methods — batch threads can safely log concurrently without corrupting the file
- **ExecutorService** thread pool sized to `Runtime.getRuntime().availableProcessors()` — maximises parallelism without thread overhead
- **IV stored in output file** — first 16 bytes of every `.enc` file are the IV; extracted automatically on decrypt

---

## 📊 OOP Concepts Demonstrated

| Concept | Where |
|---------|-------|
| Encapsulation | `EncryptionEngine` — `key` and `algorithm` are `private final` |
| Abstraction | `FileHandler.ProgressCallback` interface |
| Single Responsibility | Each class has one job |
| Anonymous classes | All event listeners in `GUIDashboard` |
| Inner interface | `ProgressCallback` defined inside `FileHandler` |
| Thread safety | `synchronized` methods in `ActivityLogger` |
| Concurrency | `ExecutorService` + `Future` in `FileHandler.doBatch()` |

---

## 🗺️ Roadmap

- [x] M1 — XOR encryption engine
- [x] M2 — AES-256 with batch mode
- [ ] M3 — Swing GUI dashboard *(in progress — 75%)*
- [ ] M4 — Firebase cloud sync via `HttpURLConnection` + OAuth2

---

## ⚠️ Disclaimer

The XOR implementation in this project is for **educational demonstration only**. It is not suitable for protecting sensitive data in production environments. For real-world use, always rely on established cryptographic libraries and follow current security best practices.

---

## 👨‍💻 Author

**Arshpreet Singh**  
Roll No: S25CSEU0980  
B.Tech CSE, Semester 2  
SCSET, Bennett University  
Email: S25CSEU0980@bennett.edu.in

**Team:** V8 Logic Systems  
**GitHub:** [github.com/Arsh-2k/The-Logic-Gate-Vault](https://github.com/Arsh-2k/The-Logic-Gate-Vault)

---

## 🙏 Acknowledgments

- Faculty: Divya Rani (Course) · Marvi Jasrotia (Lab)
- Java Cryptography Architecture (JCA) documentation
- Bennett University, SCSET

---

<div align="center">

**Built with ☕ by V8 Logic Systems · Bennett University · 2025–26**

[![GitHub](https://img.shields.io/badge/GitHub-Arsh--2k-181717?style=flat&logo=github)](https://github.com/Arsh-2k/The-Logic-Gate-Vault)

</div>