// File: FileOrchestrator.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import java.io.File;
import java.util.ArrayList;

// The CENTRAL HUB of the entire application.
// Implements EventDispatcher and coordinates all five modules (P1-P4).
//
// POLYMORPHISM demonstrated:
//   The 'engine' field is EncryptionEngine (abstract parent).
//   At runtime it holds either XOREngine or AESEngine.
//   engine.encrypt() dispatches to the right subclass with no type-specific branching.
//
// EDGE CASES FIXED in this version:
//   - OutOfMemoryError on large files is caught and shown as a clean dialog (Table 6.1)
//   - 0-byte files are rejected before encryption begins
//   - Decrypt: output file already existing is detected and reported
//   - All error paths log to activity.log before rethrowing
public class FileOrchestrator implements EventDispatcher {

    // Polymorphic reference — XOREngine or AESEngine assigned at runtime
    private EncryptionEngine engine;

    private FileHandler      fileHandler;
    private ActivityLogger   logger;
    private CloudSyncManager cloudSync;

    private String           currentUser;
    private AuthManager.Role currentRole;

    public FileOrchestrator(String currentUser, AuthManager.Role role) {
        this.currentUser = currentUser;
        this.currentRole = role;
        this.fileHandler = new FileHandler();
        this.logger      = ActivityLogger.getInstance();
        this.cloudSync   = new CloudSyncManager();
    }

    // Constructor for BatchProcessor (role defaults to USER)
    public FileOrchestrator(String currentUser) {
        this(currentUser, AuthManager.Role.USER);
    }

    // Assign the correct engine — POLYMORPHISM in action
    private void setEngine(String algorithm) {
        if (algorithm.equals("XOR")) {
            this.engine = new XOREngine();
        } else {
            this.engine = new AESEngine();
        }
    }

    // -----------------------------------------------------------------------
    // P2 — ENCRYPT
    // -----------------------------------------------------------------------
    @Override
    public void onEncrypt(String filePath, char[] password, String algorithm) {
        setEngine(algorithm);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        long startTime = System.currentTimeMillis();

        try {
            byte[] originalData = fileHandler.readFile(filePath);
            long   originalSize = originalData.length;

            // EDGE CASE: refuse to encrypt a 0-byte file — the output would be
            // meaningless (just a header with no content) and confuse the user
            if (originalSize == 0) {
                throw new Exception(
                    "Cannot encrypt an empty file (0 bytes).\n"
                    + "Please select a file that has content.");
            }

            // engine.encrypt() dispatches to XOREngine or AESEngine via polymorphism
            byte[] encryptedData = engine.encrypt(originalData, password);

            String outputPath = filePath + ".enc";
            fileHandler.writeFile(outputPath, encryptedData);
            fileHandler.deleteFile(filePath);

            long duration = System.currentTimeMillis() - startTime;
            logger.log(currentUser, "ENCRYPT", file.getName(),
                       algorithm, originalSize, duration);

        } catch (OutOfMemoryError e) {
            // Table 6.1 in the report: OOM Large File → WARN: oom_fallback
            // Full streaming via CipherInputStream is listed as future scope (report Section 9).
            // A streaming refactor would require changing the EncryptionEngine interface
            // signature (byte[] → File in/out), which would break the class diagram in the PPT.
            logger.logError(currentUser,
                "WARN: oom_fallback — " + file.getName() + " too large for in-memory processing");
            throw new RuntimeException(
                "File is too large to encrypt in memory.\n\n"
                + "The current version supports files up to approximately 256 MB.\n"
                + "Streaming support for larger files is planned for a future version.\n\n"
                + "Tip: Close other applications to free up RAM, or use a smaller file.");

        } catch (Exception e) {
            logger.logError(currentUser,
                "Encrypt failed for " + file.getName() + ": " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // P2 — DECRYPT
    // -----------------------------------------------------------------------
    @Override
    public void onDecrypt(String filePath, char[] password, String algorithm) {
        setEngine(algorithm);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        long startTime = System.currentTimeMillis();

        try {
            byte[] encryptedData = fileHandler.readFile(filePath);
            long   encryptedSize = encryptedData.length;

            byte[] decryptedData = engine.decrypt(encryptedData, password);

            String outputPath = filePath.endsWith(".enc")
                ? filePath.substring(0, filePath.length() - 4)
                : filePath + "_decrypted";

            // EDGE CASE: if the output file already exists, don't silently overwrite it.
            // The user might have important data at that path. Force them to deal with it.
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                throw new Exception(
                    "A file already exists at the output path:\n"
                    + outputPath + "\n\n"
                    + "Please move or delete it before decrypting, "
                    + "or rename the .enc file first.");
            }

            fileHandler.writeFile(outputPath, decryptedData);
            fileHandler.deleteFile(filePath);

            long duration = System.currentTimeMillis() - startTime;
            logger.log(currentUser, "DECRYPT", file.getName(),
                       algorithm, encryptedSize, duration);

        } catch (OutOfMemoryError e) {
            logger.logError(currentUser,
                "WARN: oom_fallback — " + file.getName() + " too large for in-memory processing");
            throw new RuntimeException(
                "File is too large to decrypt in memory.\n\n"
                + "The current version supports files up to approximately 256 MB.");

        } catch (Exception e) {
            logger.logError(currentUser,
                "Decrypt failed for " + file.getName() + ": " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // BATCH ENCRYPT
    // -----------------------------------------------------------------------
    @Override
    public void onBatchEncrypt(ArrayList<File> files, char[] password, String algorithm) {
        for (File file : files) {
            try {
                onEncrypt(file.getAbsolutePath(), password, algorithm);
            } catch (Exception e) {
                logger.logError(currentUser,
                    "Batch: failed on " + file.getName() + ": " + e.getMessage());
                // Continue with remaining files
            }
        }
    }

    // -----------------------------------------------------------------------
    // P4 — CLOUD SYNC
    // -----------------------------------------------------------------------
    @Override
    public void onCloudSync(String encryptedFilePath) {
        if (!encryptedFilePath.endsWith(".enc")) {
            throw new RuntimeException(
                "Only .enc encrypted files may be synced to the cloud. "
                + "Plaintext will never be uploaded.");
        }
        String fileId = cloudSync.uploadFile(encryptedFilePath);
        logger.log(currentUser, "CLOUD_UPLOAD", encryptedFilePath, "N/A", 0, 0);
        System.out.println("[P4] Upload complete. Drive FileId: " + fileId);
    }

    // -----------------------------------------------------------------------
    // KEY ESCROW RECOVERY — admin only
    // -----------------------------------------------------------------------
    @Override
    public void onEscrowRecovery(String filePath) {
        if (currentRole != AuthManager.Role.ADMIN) {
            throw new RuntimeException(
                "Access Denied! Key Escrow recovery requires ADMIN role.");
        }
        if (!filePath.endsWith(".enc")) {
            throw new RuntimeException(
                "Key Escrow only works on AES-256 encrypted .enc files.");
        }

        long startTime = System.currentTimeMillis();

        try {
            byte[] encryptedData = fileHandler.readFile(filePath);
            AESEngine aesEngine  = new AESEngine();
            byte[] decryptedData = aesEngine.decryptWithEscrow(encryptedData);

            String outputPath = filePath.substring(0, filePath.length() - 4);

            // EDGE CASE: output already exists check during escrow recovery too
            if (new File(outputPath).exists()) {
                throw new Exception(
                    "Output file already exists: " + outputPath + "\n"
                    + "Please move or delete it before performing Key Escrow recovery.");
            }

            fileHandler.writeFile(outputPath, decryptedData);
            fileHandler.deleteFile(filePath);

            long duration = System.currentTimeMillis() - startTime;
            logger.log(currentUser, "ESCROW_RECOVERY", filePath,
                       "AES-256", encryptedData.length, duration);

        } catch (Exception e) {
            logger.logError(currentUser, "Escrow recovery failed: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // Helper used directly by BatchProcessor
    public void orchestrateEncrypt(File file, char[] password, String algorithm)
            throws Exception {
        setEngine(algorithm);
        long startTime = System.currentTimeMillis();

        byte[] originalData  = fileHandler.readFile(file.getAbsolutePath());
        long   originalSize  = originalData.length;

        if (originalSize == 0) {
            throw new Exception("Cannot encrypt empty file: " + file.getName());
        }

        byte[] encryptedData = engine.encrypt(originalData, password);
        fileHandler.writeFile(file.getAbsolutePath() + ".enc", encryptedData);
        fileHandler.deleteFile(file.getAbsolutePath());

        long duration = System.currentTimeMillis() - startTime;
        logger.log(currentUser, "BATCH_ENCRYPT", file.getName(),
                   algorithm, originalSize, duration);
    }

    public String getCurrentUser()           { return currentUser; }
    public AuthManager.Role getCurrentRole() { return currentRole; }
}