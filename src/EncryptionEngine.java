import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// EncryptionEngine.java
// M1 - XOR encryption (O(n), self-inverse)
// M2 - AES-256/CBC/PKCS5 encryption
// Arshpreet Singh | S25CSEU0980

public class EncryptionEngine {

    private final String algorithm;
    private final byte[] key;

    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    // algorithm: "XOR" or "AES"
    // password: the key entered by the user
    public EncryptionEngine(String algorithm, String password) throws Exception {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        this.algorithm = algorithm.toUpperCase().trim();
        if (this.algorithm.equals("AES")) {
            this.key = deriveAESKey(password);
        } else {
            this.key = password.getBytes("UTF-8");
        }
    }

    public byte[] encrypt(byte[] data) throws Exception {
        if (algorithm.equals("XOR")) {
            return xorBytes(data);
        }
        return aesEncrypt(data);
    }

    public byte[] decrypt(byte[] data) throws Exception {
        if (algorithm.equals("XOR")) {
            return xorBytes(data);
        }
        return aesDecrypt(data);
    }

    // XOR each byte of data with the corresponding key byte (key cycles)
    // plainByte XOR keyByte = cipherByte
    // cipherByte XOR keyByte = plainByte  (same operation decrypts)
    public byte[] xorBytes(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    // AES-256 CBC encrypt
    // generates a random 16-byte IV, prepends it to output: [IV][ciphertext]
    private byte[] aesEncrypt(byte[] data) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);

        // store IV at the start so decrypt can extract it
        byte[] output = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, output, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, output, IV_SIZE, encrypted.length);
        return output;
    }

    // AES-256 CBC decrypt
    // reads IV from first 16 bytes, then decrypts the rest
    private byte[] aesDecrypt(byte[] data) throws Exception {
        if (data.length <= IV_SIZE) {
            throw new IllegalArgumentException(
                    "This file does not appear to be AES-encrypted.");
        }
        byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(data, IV_SIZE, data.length);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        return cipher.doFinal(cipherText);
    }

    // SHA-256 hash of password = 32-byte AES key
    private byte[] deriveAESKey(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(password.getBytes("UTF-8"));
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public byte[] getKey() {
        return key;
    }
}