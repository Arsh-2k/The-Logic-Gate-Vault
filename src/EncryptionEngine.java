import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// EncryptionEngine.java
// Handles XOR (M1) and AES-256/CBC (M2) encrypt/decrypt
// Arshpreet Singh | S25CSEU0980

public class EncryptionEngine {

    private final String algorithm; // "XOR" or "AES"
    private final byte[] key;       // key bytes derived from the password

    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16; // AES IV is always 16 bytes

    // Constructor: derives the encryption key from the user's password
    public EncryptionEngine(String algorithm, String password) throws Exception {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        this.algorithm = algorithm.toUpperCase().trim();
        if (this.algorithm.equals("AES")) {
            this.key = deriveAESKey(password);
        } else {
            // For XOR, use the password bytes directly as the key
            this.key = password.getBytes("UTF-8");
        }
    }

    // Encrypt raw file bytes - picks XOR or AES depending on the algorithm field
    public byte[] encrypt(byte[] data) throws Exception {
        if (algorithm.equals("XOR")) {
            return xorBytes(data);
        }
        return aesEncrypt(data);
    }

    // Decrypt raw file bytes - picks XOR or AES depending on the algorithm field
    public byte[] decrypt(byte[] data) throws Exception {
        if (algorithm.equals("XOR")) {
            // XOR is self-inverse: running it again with the same key decrypts the data
            return xorBytes(data);
        }
        return aesDecrypt(data);
    }

    // XOR: each byte of data is XORed with the matching key byte
    // If the key is shorter than the file, it repeats using modulo
    // plainByte XOR keyByte = cipherByte, and cipherByte XOR keyByte = plainByte
    public byte[] xorBytes(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    // AES-256 CBC encrypt
    // Generates a fresh random 16-byte IV for every encryption call
    // Output layout: [IV - 16 bytes][ciphertext]
    private byte[] aesEncrypt(byte[] data) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);

        // Prepend the IV to the ciphertext so decryption can extract it
        byte[] output = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, output, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, output, IV_SIZE, encrypted.length);
        return output;
    }

    // AES-256 CBC decrypt
    // First 16 bytes are the IV, the rest is the ciphertext
    private byte[] aesDecrypt(byte[] data) throws Exception {
        if (data.length <= IV_SIZE) {
            throw new IllegalArgumentException(
                    "This file does not look like an AES-encrypted file.");
        }
        byte[] iv         = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(data, IV_SIZE, data.length);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        return cipher.doFinal(cipherText);
    }

    // SHA-256 hash of the password gives us exactly 32 bytes = the 256-bit AES key
    private byte[] deriveAESKey(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(password.getBytes("UTF-8"));
    }

    // Returns "XOR" or "AES" - used by FileHandler when logging operations
    public String getAlgorithm() {
        return algorithm;
    }

    // NOTE: getKey() was removed - it was declared but never called from outside this class.
    // Keeping unused public methods causes yellow "unused method" warnings in VS Code.
}