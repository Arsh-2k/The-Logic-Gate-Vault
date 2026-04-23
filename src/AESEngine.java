// File: AESEngine.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

// Module M2 — AES-256 Engine.
// Extends EncryptionEngine — demonstrates INHERITANCE and POLYMORPHISM.
// Uses AES-256/CBC/PKCS5Padding via the Java Cryptography Extension (JCE).
//
// KEY ESCROW: Every encrypted file stores the session key wrapped with the
// admin's master key at header bytes 16-63. This lets the admin decrypt any
// file without knowing the original user's password. (P2 ↔ D2 admin.cfg link)
//
// AES .enc file format (96-byte header):
//   Bytes  0-15 : IV — random 16 bytes (SecureRandom, fresh per encryption)
//   Bytes 16-63 : Session key wrapped with admin key (Key Escrow, 48 bytes)
//   Bytes 64-95 : SHA-256 checksum of original plaintext (32 bytes)
//   Bytes 96+   : AES/CBC/PKCS5 ciphertext
public class AESEngine extends EncryptionEngine {

    // Admin's escrow password. In production this would be read from data/admin.cfg.
    // Hardcoded here for the project demo — the path constant is kept for DFD alignment.
    private static final char[] ADMIN_ESCROW_PASSWORD = "Admin@123".toCharArray();
    public  static final String ADMIN_CFG_PATH        = "data/admin.cfg";

    public AESEngine() {
        super("AES-256");
    }

    // Derive a 32-byte SecretKeySpec from a char[] password.
    // Calls deriveKey() from the parent EncryptionEngine — demonstrates inheritance.
    private SecretKeySpec buildKeySpec(char[] password) throws Exception {
        byte[] keyBytes = deriveKey(password); // inherited concrete method
        SecretKeySpec spec = new SecretKeySpec(keyBytes, "AES");
        Arrays.fill(keyBytes, (byte) 0); // memory security — zero key bytes after wrapping
        return spec;
    }

    // Wrap (AES-encrypt) the session key using the admin's master key.
    // Result is 48 bytes: 32-byte key → PKCS5 padded to next 16-byte block.
    // Stored at header bytes 16-63 for Key Escrow admin recovery.
    private byte[] wrapKeyForEscrow(byte[] sessionKeyBytes) throws Exception {
        SecretKeySpec adminKey = buildKeySpec(ADMIN_ESCROW_PASSWORD);
        byte[] escrowIV = new byte[16]; // fixed all-zeros IV for deterministic wrapping
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, adminKey, new IvParameterSpec(escrowIV));
        byte[] wrapped = cipher.doFinal(sessionKeyBytes);
        Arrays.fill(sessionKeyBytes, (byte) 0); // memory security — zero session key copy
        return wrapped; // 48 bytes
    }

    // Unwrap the session key from escrow using the admin's master key.
    private byte[] unwrapKeyFromEscrow(byte[] wrappedKey) throws Exception {
        SecretKeySpec adminKey = buildKeySpec(ADMIN_ESCROW_PASSWORD);
        byte[] escrowIV = new byte[16]; // same fixed IV used during wrapping
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, adminKey, new IvParameterSpec(escrowIV));
        return cipher.doFinal(wrappedKey); // returns original 32-byte session key
    }

    @Override
    public byte[] encrypt(byte[] data, char[] password) throws Exception {
        // Step 1: Derive the 32-byte session key from the user's password
        SecretKeySpec sessionKey = buildKeySpec(password);

        // Step 2: Generate a fresh cryptographically random 16-byte IV.
        // A new IV every call means the same file encrypted twice produces
        // different ciphertext — this is called "semantic security" for CBC mode.
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        // Step 3: Wrap the session key for Key Escrow (admin recovery).
        // getEncoded() makes a copy; wrapKeyForEscrow() zeroes that copy.
        byte[] wrappedKey = wrapKeyForEscrow(sessionKey.getEncoded());

        // Step 4: SHA-256 checksum of original plaintext — for integrity check
        byte[] checksum = MessageDigest.getInstance("SHA-256").digest(data); // 32 bytes

        // Step 5: AES-256/CBC/PKCS5Padding encryption
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(data);

        // Step 6: Assemble 96-byte header + ciphertext
        byte[] result = new byte[16 + 48 + 32 + ciphertext.length];
        System.arraycopy(iv,         0, result,  0, 16);
        System.arraycopy(wrappedKey, 0, result, 16, 48);
        System.arraycopy(checksum,   0, result, 64, 32);
        System.arraycopy(ciphertext, 0, result, 96, ciphertext.length);

        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, char[] password) throws Exception {
        // 97 = 96-byte header + minimum 1 byte of ciphertext
        if (data.length < 97) {
            throw new Exception(
                "File header corrupted — too small to be a valid AES-encrypted file.");
        }

        // Extract the three header sections
        byte[] iv             = new byte[16];
        byte[] storedChecksum = new byte[32];
        System.arraycopy(data,  0, iv,             0, 16);
        System.arraycopy(data, 64, storedChecksum,  0, 32);

        byte[] ciphertext = new byte[data.length - 96];
        System.arraycopy(data, 96, ciphertext, 0, ciphertext.length);

        // Derive session key and decrypt
        SecretKeySpec sessionKey = buildKeySpec(password);
        byte[] plaintext;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, new IvParameterSpec(iv));
            plaintext = cipher.doFinal(ciphertext);
        } catch (javax.crypto.BadPaddingException e) {
            // Wrong key — AES couldn't cleanly remove PKCS5 padding
            throw new Exception("Wrong password! AES decryption failed.");
        } catch (javax.crypto.IllegalBlockSizeException e) {
            throw new Exception("File is corrupted — invalid ciphertext block size.");
        }

        // Verify SHA-256 checksum of decrypted output
        byte[] computedChecksum = MessageDigest.getInstance("SHA-256").digest(plaintext);
        if (!Arrays.equals(storedChecksum, computedChecksum)) {
            throw new Exception("SHA-256 integrity check FAILED — file may have been tampered with.");
        }

        return plaintext;
    }

    // KEY ESCROW RECOVERY — admin only.
    // Decrypts any AES-256 file using the admin's master key WITHOUT the user's password.
    // Called via FileOrchestrator.onEscrowRecovery() after role check.
    public byte[] decryptWithEscrow(byte[] data) throws Exception {
        if (data.length < 97) {
            throw new Exception("File header corrupted — cannot perform Key Escrow recovery.");
        }

        byte[] iv             = new byte[16];
        byte[] wrappedKey     = new byte[48];
        byte[] storedChecksum = new byte[32];
        System.arraycopy(data,  0, iv,             0, 16);
        System.arraycopy(data, 16, wrappedKey,      0, 48);
        System.arraycopy(data, 64, storedChecksum,  0, 32);

        byte[] ciphertext = new byte[data.length - 96];
        System.arraycopy(data, 96, ciphertext, 0, ciphertext.length);

        // Unwrap the original session key using the admin's escrow key
        byte[] recoveredKeyBytes = unwrapKeyFromEscrow(wrappedKey);
        SecretKeySpec recoveredKey = new SecretKeySpec(recoveredKeyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, recoveredKey, new IvParameterSpec(iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        // Memory security: zero the recovered session key bytes
        Arrays.fill(recoveredKeyBytes, (byte) 0); // memory security — zero recovered key

        // Verify integrity even during escrow recovery
        byte[] computedChecksum = MessageDigest.getInstance("SHA-256").digest(plaintext);
        if (!Arrays.equals(storedChecksum, computedChecksum)) {
            throw new Exception("Integrity check FAILED during Key Escrow recovery.");
        }

        return plaintext;
    }
}
