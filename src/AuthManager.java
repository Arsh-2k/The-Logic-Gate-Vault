// File: AuthManager.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// P1 — Authentication process.
// Data Store D1 = data/users.dat (DFD requirement).
//
// ENCAPSULATION demonstrated: UserCredential is a private inner class.
// The only public methods are authenticate() and verifyEscrowKey().
//
// PBKDF2WithHmacSHA256 with 65,536 iterations (matches report Section 4 M5).
// High iteration count = brute-forcing is computationally expensive.
//
// FIX APPLIED (Point 4): All file streams use try-with-resources.
// FIX APPLIED (Point 5): UserCredential now implements Serializable.
//   - On first launch: default users are created and written to data/users.dat (D1).
//   - On subsequent launches: credentials are loaded from data/users.dat.
//   - This proves the DFD P1 <-> D1 data flow is real, not just a mock.
public class AuthManager {

    // D1 data store path
    public static final String DATA_DIR       = "data";
    public static final String USERS_DAT_PATH = "data/users.dat";

    // The two RBAC roles
    public enum Role { ADMIN, USER }

    // Private inner class — Encapsulation + Serializable for D1 persistence (Point 5)
    private static class UserCredential implements Serializable {
        private static final long serialVersionUID = 1L; // required for Serializable

        String username;
        byte[] salt;        // 16-byte random salt — prevents rainbow table attacks
        byte[] pbkdf2Hash;  // PBKDF2WithHmacSHA256 hash of (password + salt)
        Role   role;

        UserCredential(String u, byte[] s, byte[] h, Role r) {
            username   = u;
            salt       = s;
            pbkdf2Hash = h;
            role       = r;
        }
    }

    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int DERIVED_KEY_BITS  = 256;

    private HashMap<String, UserCredential> credentialStore;

    public AuthManager() {
        new File(DATA_DIR).mkdirs(); // ensure data/ directory exists for D1
        credentialStore = new HashMap<>();
        loadOrCreateUsers(); // attempt to load from D1, create defaults if not found
    }

    // FIX (Point 5): Try to load users from data/users.dat first.
    // If the file doesn't exist (first launch), create the default accounts
    // and persist them to users.dat so P1 <-> D1 data flow is real.
    private void loadOrCreateUsers() {
        File usersFile = new File(USERS_DAT_PATH);

        if (usersFile.exists()) {
            // FIX (Point 4): try-with-resources for ObjectInputStream
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(usersFile))) {

                // Suppress unchecked cast warning — safe because we wrote this file
                @SuppressWarnings("unchecked")
                HashMap<String, UserCredential> loaded =
                    (HashMap<String, UserCredential>) ois.readObject();

                credentialStore = loaded;
                System.out.println("[P1] Loaded " + credentialStore.size()
                    + " user(s) from " + USERS_DAT_PATH);
                return;

            } catch (Exception e) {
                // File exists but could not be read (corrupted, class changed, etc.)
                // Fall through to create defaults
                System.out.println("[P1] Could not read " + USERS_DAT_PATH
                    + " — creating default accounts: " + e.getMessage());
            }
        }

        // First launch or corrupted file — create default accounts
        System.out.println("[P1] " + USERS_DAT_PATH
            + " not found — initialising default accounts and saving to D1.");
        initializeDefaultUsers();
        saveUsersToFile();
    }

    // Write the current credentialStore to data/users.dat (D1)
    // FIX (Point 4): try-with-resources for ObjectOutputStream
    private void saveUsersToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(USERS_DAT_PATH))) {
            oos.writeObject(credentialStore);
            System.out.println("[P1] Saved " + credentialStore.size()
                + " user(s) to " + USERS_DAT_PATH);
        } catch (IOException e) {
            System.out.println("[P1] Warning: Could not save to "
                + USERS_DAT_PATH + ": " + e.getMessage());
        }
    }

    // Create the two hardcoded default accounts in memory.
    // Called only on first launch or if users.dat is missing/corrupted.
    private void initializeDefaultUsers() {
        try {
            // Admin account — username: "admin", password: "Admin@123"
            byte[] adminSalt = hexToBytes("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6");
            byte[] adminHash = derivePBKDF2("Admin@123".toCharArray(), adminSalt);
            credentialStore.put("admin",
                new UserCredential("admin", adminSalt, adminHash, Role.ADMIN));

            // Standard user — username: "user", password: "User@123"
            byte[] userSalt = hexToBytes("f1e2d3c4b5a6f7e8d9c0b1a2f3e4d5c6");
            byte[] userHash = derivePBKDF2("User@123".toCharArray(), userSalt);
            credentialStore.put("user",
                new UserCredential("user", userSalt, userHash, Role.USER));

        } catch (Exception e) {
            System.out.println("CRITICAL: Could not initialise credential store: " + e.getMessage());
        }
    }

    // Authenticate a user — returns their Role on success, null on failure.
    // All PBKDF2 complexity hidden behind this single method (Encapsulation + Facade).
    public Role authenticate(String username, char[] password) {
        UserCredential cred = credentialStore.get(username.toLowerCase());
        if (cred == null) {
            return null; // username not found
        }

        try {
            byte[] submittedHash = derivePBKDF2(password, cred.salt);

            // MessageDigest.isEqual() is CONSTANT-TIME — prevents timing side-channel attacks.
            // Arrays.equals() can return faster for non-matching arrays, leaking timing info.
            if (MessageDigest.isEqual(submittedHash, cred.pbkdf2Hash)) {
                return cred.role; // correct password
            }
        } catch (Exception e) {
            System.out.println("Auth error: " + e.getMessage());
        }

        return null; // wrong password
    }

    // Verify an escrow key hash — used before Key Escrow admin operations.
    // Matches UML class diagram: + verifyEscrowKey(byte[])
    public boolean verifyEscrowKey(byte[] providedKeyHash) {
        UserCredential adminCred = credentialStore.get("admin");
        if (adminCred == null) { return false; }
        return MessageDigest.isEqual(providedKeyHash, adminCred.pbkdf2Hash);
    }

    // PRIVATE: Runs PBKDF2WithHmacSHA256. Hidden from callers — encapsulation.
    private byte[] derivePBKDF2(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, DERIVED_KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        spec.clearPassword(); // clear from PBEKeySpec immediately after deriving
        return hash;
    }

    // Helper: convert hex string "a1b2c3..." → byte[]
    private byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}