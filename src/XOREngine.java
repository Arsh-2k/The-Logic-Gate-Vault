// File: XOREngine.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import java.security.MessageDigest;
import java.util.Arrays;

// Module M1 — XOR Engine.
// Extends EncryptionEngine — demonstrates INHERITANCE (OOP Pillar 2).
//
// SECURITY NOTE: XOR with a short repeating key is a weak Vigenere-like
// cipher — it is included as a PEDAGOGICAL DEMO only, not for real data.
// Use AES-256 for anything sensitive.
//
// XOR has NO built-in integrity checking — any key produces some output with
// no error. We compensate with an external SHA-256 checksum prepended to the
// output so we can detect wrong passwords after decryption.
//
// XOR file format:
//   Bytes  0-31 : SHA-256 checksum of original plaintext (32 bytes)
//   Bytes 32+   : XOR ciphertext
public class XOREngine extends EncryptionEngine {

    public XOREngine() {
        super("XOR");
    }

    // Compute SHA-256 hash of a byte array — always 32 bytes
    private byte[] computeSHA256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }

    // Expand the password to exactly 'length' bytes by cycling through it.
    // Example: password "abc", length 7 → [a, b, c, a, b, c, a]
    private byte[] expandKey(char[] password, int length) {
        byte[] keySource = new byte[password.length];
        for (int i = 0; i < password.length; i++) {
            keySource[i] = (byte) password[i];
        }

        byte[] expandedKey = new byte[length];
        for (int i = 0; i < length; i++) {
            expandedKey[i] = keySource[i % keySource.length]; // cycle with modulo
        }

        Arrays.fill(keySource, (byte) 0); // memory security — zero the source array
        return expandedKey;
    }

    @Override
    public byte[] encrypt(byte[] data, char[] password) throws Exception {
        // Step 1: Compute SHA-256 checksum of the ORIGINAL plaintext.
        // This is an EXTERNAL integrity tag — XOR itself has no integrity checking.
        byte[] checksum = computeSHA256(data);

        // Step 2: Expand password to the length of the data
        byte[] keyBytes = expandKey(password, data.length);

        // Step 3: XOR each byte — C[i] = P[i] XOR K[i % keyLength]
        // This is O(n) — a single pass through the data. Very fast!
        byte[] ciphertext = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            ciphertext[i] = (byte) (data[i] ^ keyBytes[i]);
        }
        Arrays.fill(keyBytes, (byte) 0); // memory security — zero expanded key

        // Step 4: Output = [32-byte checksum] + [ciphertext]
        byte[] result = new byte[32 + ciphertext.length];
        System.arraycopy(checksum,   0, result,  0, 32);
        System.arraycopy(ciphertext, 0, result, 32, ciphertext.length);

        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, char[] password) throws Exception {
        // Minimum valid file: 32 bytes checksum + at least 1 byte ciphertext
        if (data.length < 33) {
            throw new Exception(
                "File header corrupted — too small to be a valid XOR-encrypted file.");
        }

        // Step 1: Extract the stored 32-byte checksum from the header
        byte[] storedChecksum = new byte[32];
        System.arraycopy(data, 0, storedChecksum, 0, 32);

        // Step 2: Extract the ciphertext (everything after the 32-byte header)
        byte[] ciphertext = new byte[data.length - 32];
        System.arraycopy(data, 32, ciphertext, 0, ciphertext.length);

        // Step 3: XOR decrypt — XOR is its own inverse: (A XOR K) XOR K = A
        byte[] keyBytes = expandKey(password, ciphertext.length);
        byte[] plaintext = new byte[ciphertext.length];
        for (int i = 0; i < ciphertext.length; i++) {
            plaintext[i] = (byte) (ciphertext[i] ^ keyBytes[i]);
        }
        Arrays.fill(keyBytes, (byte) 0); // memory security — zero expanded key

        // Step 4: Recompute SHA-256 and compare with the stored checksum.
        // Correct password → hashes match → file restored correctly.
        // Wrong password  → decrypted garbage → hashes differ → error thrown.
        byte[] computedChecksum = computeSHA256(plaintext);

        if (!Arrays.equals(storedChecksum, computedChecksum)) {
            throw new Exception(
                "Integrity check FAILED — wrong password or file has been tampered with. "
                + "(SHA-256 checksum mismatch)");
        }

        return plaintext;
    }
}
