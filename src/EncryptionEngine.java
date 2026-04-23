// File: EncryptionEngine.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import java.security.MessageDigest;
import java.util.Arrays;

// Abstract base class — demonstrates ABSTRACTION (OOP Pillar 1).
// Defines WHAT every encryption engine must do.
// XOREngine and AESEngine extend this and define HOW.
public abstract class EncryptionEngine {

    // The algorithm name, e.g. "XOR" or "AES-256"
    protected String algorithm;

    public EncryptionEngine(String algorithm) {
        this.algorithm = algorithm;
    }

    // ABSTRACT — every subclass MUST implement these two methods
    public abstract byte[] encrypt(byte[] data, char[] password) throws Exception;
    public abstract byte[] decrypt(byte[] data, char[] password) throws Exception;

    // CONCRETE method shared by all subclasses via inheritance.
    // Derives a 32-byte AES key from a password using SHA-256.
    // Shown in UML class diagram as: + deriveKey(char[]): byte[] [concrete]
    public byte[] deriveKey(char[] password) throws Exception {
        byte[] passwordBytes = new byte[password.length];
        for (int i = 0; i < password.length; i++) {
            passwordBytes[i] = (byte) password[i];
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = md.digest(passwordBytes); // always 32 bytes = 256 bits
        Arrays.fill(passwordBytes, (byte) 0); // memory security — zero temp buffer
        return keyBytes;
    }

    // Returns the algorithm name — inherited by all subclasses for free
    public String getAlgorithmName() {
        return algorithm;
    }
}
