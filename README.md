# 🔐 The Logic-Gate Vault
### A Dynamic Data Encryption Engine

![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

**A progressive encryption suite demonstrating the evolution from fundamental bitwise logic to industry-standard cryptographic implementations.**

---

## 📋 Project Overview

**The Logic-Gate Vault** is a multi-layered security application developed as part of the B.Tech Computer Science & Engineering curriculum at Bennett University. This project serves as a "glass box" educational tool, illustrating how discrete mathematical structures and concurrent programming principles can be scaled into a robust encryption engine.

### 🎯 Core Objectives
- Demonstrate progression from XOR bitwise operations to AES-256 encryption
- Implement file I/O for complex binary formats (Excel, Word, PDF)
- Integrate GUI design patterns with the Java Swing framework
- Apply concurrent programming for responsive user interfaces
- Implement cloud synchronization capabilities

---

## ✨ Features

### Phase 1: Logic Engine ✅
- [x] Custom XOR encryption implementation
- [x] Rolling key mechanism using modulo operations
- [x] Text file encryption/decryption
- [x] Console-based interface

### Phase 2: Cryptographic Engine 🚧
- [ ] AES-256 standard encryption integration
- [ ] Binary file support (.xlsx, .docx, .pdf)
- [ ] Secure key generation and management
- [ ] Enhanced GUI with tabbed interface

### Phase 3: Cloud & Concurrency 📅
- [ ] Asynchronous cloud synchronization
- [ ] Multi-threaded encryption operations
- [ ] Forensic audit logging system
- [ ] REST API integration

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 21 (OpenJDK) |
| **GUI Framework** | Swing (javax.swing) |
| **Cryptography** | Java Cryptography Extension (JCE) |
| **Concurrency** | java.util.concurrent |
| **Build Tool** | Maven / Manual compilation |
| **Version Control** | Git + GitHub |

---

## 📦 Dependencies

```java
// Core Java Libraries
import javax.swing.*;           // GUI components
import java.awt.*;              // Layout and styling
import javax.crypto.*;          // AES encryption
import java.security.*;         // Key generation
import java.io.*;               // File operations
import java.net.*;              // Cloud sync
import java.util.concurrent.*; // Threading
import java.util.Base64;       // Encoding
import java.time.LocalDateTime; // Audit logging
```

---

## 🚀 Quick Start

### Prerequisites
- Java Development Kit (JDK) 21 or higher
- Windows, macOS, or Linux operating system

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Arsh-2k/The-Logic-Gate-Vault.git
   cd The-Logic-Gate-Vault
   ```

2. **Compile the project**
   
   **Windows:**
   ```cmd
   compile.bat
   ```
   
   **macOS/Linux:**
   ```bash
   ./compile.sh
   ```

3. **Run the application**
   
   **Windows:**
   ```cmd
   run.bat
   ```
   
   **macOS/Linux:**
   ```bash
   ./run.sh
   ```

### Manual Compilation

```bash
# Compile all Java files
javac -d bin src/com/v8logic/vault/*.java

# Run the application
java -cp bin com.v8logic.vault.Main
```

---

## 📖 Usage

### Console Mode (Phase 1)

```
╔════════════════════════════════════════════╗
║    🔐 LOGIC-GATE VAULT v1.0               ║
║    XOR Encryption Engine                   ║
╚════════════════════════════════════════════╝

MAIN MENU:
1. Encrypt a Text File
2. Decrypt a Text File
3. View File Content
4. Change Encryption Key
5. Exit

Enter your choice: _
```

### Example Workflow

```bash
# Encrypt a file
1. Select option 1
2. Enter file path: examples/secret.txt
3. Encrypted file created: secret_encrypted.txt

# Decrypt the file
1. Select option 2
2. Enter file path: secret_encrypted.txt
3. Original content restored
```

---

## 📁 Project Structure

```
The-Logic-Gate-Vault/
│
├── src/
│   └── com/
│       └── v8logic/
│           └── vault/
│               ├── Main.java                 # Application entry point
│               ├── XOREncryption.java        # XOR logic implementation
│               ├── FileHandler.java          # File I/O operations
│               └── Config.java               # Configuration constants
│
├── examples/
│   ├── sample.txt                            # Test file
│   └── README.md                             # Testing instructions
│
├── docs/
│   ├── SETUP.md                              # Detailed setup guide
│   ├── LEARNING_LOG.md                       # Development journal
│   └── API_DOCUMENTATION.md                  # Code documentation
│
├── bin/                                      # Compiled .class files
│
├── run.bat                                   # Windows run script
├── compile.bat                               # Windows compile script
├── README.md                                 # This file
└── LICENSE                                   # MIT License
```

---

## 🔒 Encryption Methods

### XOR Encryption (Phase 1)
```java
// Simple XOR with rolling key
encryptedChar = originalChar XOR key[i % keyLength]
```

**Characteristics:**
- Symmetric encryption (same key for encrypt/decrypt)
- Rolling key mechanism
- Educational demonstration of bitwise operations

### AES-256 (Phase 2) 🚧
```java
// Industry-standard encryption
Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
SecretKey key = generateKey(256);
cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
```

**Characteristics:**
- 256-bit key strength
- NIST-approved algorithm
- Production-grade security

---

## 📊 Syllabus Mapping

This project addresses the following course modules:

### Module I: Fundamentals
- Advanced operators and bitwise logic
- Date-Time API (java.time)
- File I/O streams

### Module II: Object-Oriented Programming
- Exception handling (try-catch-finally)
- Interfaces and abstract classes
- Java Cryptography Extension

### Module III: Advanced Concepts
- GUI development with Swing
- Multi-threading and concurrency
- Network programming basics

---

## 🤝 Contributing

This is an academic project, but suggestions and feedback are welcome!

### Development Guidelines
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes (`git commit -m 'Add YourFeature'`)
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Arshpreet Singh**
- Roll Number: S25CSEU0980
- Email: S25CSEU0980@bennett.edu.in
- Course: B.Tech Computer Science & Engineering (Semester 2)
- Institution: Bennett University

**Team:** V8 Logic Systems

---

## 🙏 Acknowledgments

- Bennett University Faculty for project guidance
- Java Cryptography Architecture (JCA) documentation
- Open-source community for educational resources

---

## 📅 Project Development Phases

| Phase |
|-------|
| **Phase 1: Logic Engine** | 
| **Phase 2: Cryptographic Engine** | 
| **Phase 3: Cloud & Concurrency** | 

---

## ⚠️ Disclaimer

This project is developed for **educational purposes only**. The XOR encryption implementation is not suitable for production use. For real-world applications, always use proven cryptographic libraries and follow security best practices.

---

<div align="center">

**Built with ☕ and 💻 by V8 Logic Systems**

[![GitHub](https://img.shields.io/badge/GitHub-Arsh--2k-181717?style=flat&logo=github)](https://github.com/Arsh-2k)

</div>