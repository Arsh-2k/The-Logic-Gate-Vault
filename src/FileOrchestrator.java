// File: FileOrchestrator.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
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
// EDGE CASES handled:
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
    // P2 — ENCRYPT (single file, called from the GUI Encrypt button)
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
    // P2 — DECRYPT (single file, called from the GUI Decrypt button)
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
    // BATCH ENCRYPT (EventDispatcher implementation)
    // BatchProcessor calls orchestrateEncrypt() directly for per-file progress.
    // This method exists to satisfy the interface contract.
    // -----------------------------------------------------------------------
    @Override
    public void onBatchEncrypt(ArrayList<File> files, char[] password, String algorithm) {
        for (File file : files) {
            try {
                onEncrypt(file.getAbsolutePath(), password, algorithm);
            } catch (Exception e) {
                logger.logError(currentUser,
                    "Batch encrypt: failed on " + file.getName() + ": " + e.getMessage());
                // Continue with remaining files
            }
        }
    }

    // -----------------------------------------------------------------------
    // BATCH DECRYPT (EventDispatcher implementation)
    // BatchProcessor calls orchestrateDecrypt() directly for per-file progress.
    // This method exists to satisfy the interface contract.
    // -----------------------------------------------------------------------
    @Override
    public void onBatchDecrypt(ArrayList<File> files, char[] password, String algorithm) {
        for (File file : files) {
            try {
                orchestrateDecrypt(file, password, algorithm);
            } catch (Exception e) {
                logger.logError(currentUser,
                    "Batch decrypt: failed on " + file.getName() + ": " + e.getMessage());
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

    // -----------------------------------------------------------------------
    // Helper: BATCH ENCRYPT — called directly by BatchProcessor per file.
    // Using this instead of onEncrypt() lets BatchProcessor publish per-file
    // progress without going through the full single-file exception wrapper.
    // -----------------------------------------------------------------------
    public void orchestrateEncrypt(File file, char[] password, String algorithm)
            throws Exception {
        setEngine(algorithm);
        long startTime = System.currentTimeMillis();

        byte[] originalData = fileHandler.readFile(file.getAbsolutePath());
        long   originalSize = originalData.length;

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

    // -----------------------------------------------------------------------
    // Helper: BATCH DECRYPT — called directly by BatchProcessor per file.
    // Mirrors orchestrateEncrypt() but runs the decrypt pipeline.
    // Includes the same safety guards as onDecrypt() — .enc check, output-exists
    // check, and OOM catch — adapted for the batch context.
    // Uses the BATCH_DECRYPT log tag so the admin monitoring panel can
    // distinguish batch operations from single-file decryptions.
    // -----------------------------------------------------------------------
    public void orchestrateDecrypt(File file, char[] password, String algorithm)
            throws Exception {

        // Guard: batch decrypt only makes sense on .enc files.
        // Non-.enc files are skipped with a clear message rather than silently ignored.
        if (!file.getName().endsWith(".enc")) {
            throw new Exception(
                "Skipped (not a .enc file): " + file.getName()
                + " — only files ending in .enc can be decrypted.");
        }

        setEngine(algorithm);
        long startTime = System.currentTimeMillis();

        try {
            byte[] encryptedData = fileHandler.readFile(file.getAbsolutePath());
            long   encryptedSize = encryptedData.length;

            // engine.decrypt() dispatches to XOREngine or AESEngine via polymorphism
            byte[] decryptedData = engine.decrypt(encryptedData, password);

            // Derive output path by stripping the .enc extension
            String outputPath = file.getAbsolutePath()
                .substring(0, file.getAbsolutePath().length() - 4);

            // Do not silently overwrite an existing file at the output path.
            // The user's data at that path could be important — force them to resolve it.
            if (new File(outputPath).exists()) {
                throw new Exception(
                    "Output file already exists: " + outputPath + "\n"
                    + "Move or rename it before running Batch Decrypt.");
            }

            fileHandler.writeFile(outputPath, decryptedData);
            fileHandler.deleteFile(file.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;
            logger.log(currentUser, "BATCH_DECRYPT", file.getName(),
                       algorithm, encryptedSize, duration);

        } catch (OutOfMemoryError e) {
            // Consistent with Table 6.1 WARN: oom_fallback treatment
            logger.logError(currentUser,
                "WARN: oom_fallback — " + file.getName()
                + " too large for in-memory decryption");
            throw new Exception(
                "File too large to decrypt in memory: " + file.getName()
                + "\n(~256 MB limit). Close other applications and retry.");
            // All other exceptions propagate to BatchProcessor's catch block,
            // where they are logged per-file and iteration continues normally.
        }
    }

    public String getCurrentUser()           { return currentUser; }
    public AuthManager.Role getCurrentRole() { return currentRole; }
}