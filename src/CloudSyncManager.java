// File: CloudSyncManager.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
//   java -cp out MainApp

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// P4 — Cloud Sync process.
// Data Store D4 = data/.vault (DFD requirement).
//
// Provides a functional mock of Google Drive integration.
// The .vault file (D4) stores filename → driveFileId mappings as a
// Java Properties file (report Section 5.4).
//
// FIX (v4): Added public saveVaultEntry(filePath, fileId) method so
// MainApp's SwingWorker can persist the SimulatedUpload fileId —
// the SAME id shown in the progress dialog — to .vault.
// Previously, uploadFile() was called separately and generated a DIFFERENT
// fileId, causing the dialog to show id=A while .vault stored id=B.
//
// ZERO-KNOWLEDGE GUARANTEE: uploadFile() rejects non-.enc files (report §4 M4).
public class CloudSyncManager {

    public static final String DATA_DIR   = "data";
    public static final String VAULT_PATH = "data/.vault";

    private boolean isConnected;

    public CloudSyncManager() {
        new File(DATA_DIR).mkdirs();
        this.isConnected = false;
    }

    // Upload with immediate return — called by FileOrchestrator.onCloudSync().
    // Generates its own fileId and persists it.
    public String uploadFile(String encryptedFilePath) {
        if (!encryptedFilePath.endsWith(".enc")) {
            throw new IllegalArgumentException(
                "Zero-knowledge violation: only .enc files may be uploaded. "
                + "Plaintext will never be sent to the cloud.");
        }
        String mockFileId = "GD_" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        saveToVault(encryptedFilePath, mockFileId);
        System.out.println("[M4] Upload complete. Mock Drive FileId = " + mockFileId);
        return mockFileId;
    }

    // FIX (v4): Public entry point for persisting a fileId chosen by the caller.
    // MainApp.handleCloudSync() uses this so the id shown in the dialog is the
    // same id that gets written to .vault — no second random id generated.
    public void saveVaultEntry(String filePath, String fileId) {
        saveToVault(filePath, fileId);
        System.out.println("[M4] Vault entry saved: " + filePath + " → " + fileId);
    }

    // Download — looks up fileId from D4 (.vault) and fetches from Drive (mock)
    public void downloadFile(String localFilePath) {
        String fileId = loadFromVault(localFilePath);
        if (fileId == null) {
            System.out.println("[M4] No FileId in .vault for: " + localFilePath);
            return;
        }
        System.out.println("[M4] MOCK: Would download FileId '"
            + fileId + "' to: " + localFilePath);
    }

    public boolean isConnected() { return isConnected; }

    // -----------------------------------------------------------------------
    // SIMULATED UPLOAD — staged progress for the Drive Sync UI dialog.
    // MainApp creates a SimulatedUpload and drives it from a SwingWorker loop.
    // -----------------------------------------------------------------------
    public static class SimulatedUpload {

        // Stage labels and the progress % they complete to
        private static final String[] STAGE_LABELS = {
            "Initializing TLS connection...",
            "Connecting to Google Cloud servers...",
            "Verifying OAuth2 access token...",
            "Authenticating service account...",
            "Preparing encrypted payload...",
            "Uploading .enc file to Google Drive...",
            "Verifying remote checksum...",
            "Persisting FileId to data/.vault...",
            "Upload complete!"
        };
        private static final int[] STAGE_END_PERCENT = {
            8, 18, 32, 46, 58, 78, 88, 96, 100
        };

        private int    currentStage = 0;
        private String assignedFileId;

        public SimulatedUpload(String fileName) {
            // Generate the fileId once — shown in dialog AND saved to .vault
            this.assignedFileId =
                "GD_" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        }

        public int     getTotalStages()       { return STAGE_LABELS.length; }
        public boolean hasNextStage()         { return currentStage < STAGE_LABELS.length; }
        public String  getAssignedFileId()    { return assignedFileId; }

        // Advance to the next stage — call from SwingWorker.doInBackground() loop
        public StageResult nextStage() {
            if (!hasNextStage()) throw new IllegalStateException("No more stages.");
            StageResult result = new StageResult(
                STAGE_LABELS[currentStage],
                STAGE_END_PERCENT[currentStage]);
            currentStage++;
            return result;
        }

        // Simple data holder passed via SwingWorker.publish()
        public static class StageResult {
            public final String label;
            public final int    percent;
            StageResult(String label, int percent) {
                this.label   = label;
                this.percent = percent;
            }
        }
    }

    // -----------------------------------------------------------------------
    // D4 .vault persistence — private helpers
    // -----------------------------------------------------------------------
    private void saveToVault(String filePath, String fileId) {
        Properties props = loadVaultProperties();
        props.setProperty(filePath, fileId);
        try (FileWriter fw = new FileWriter(VAULT_PATH, StandardCharsets.UTF_8)) {
            props.store(fw, "Logic-Gate Vault — Google Drive FileId mappings (D4)");
        } catch (IOException e) {
            System.out.println("[M4] Warning: Could not write .vault: " + e.getMessage());
        }
    }

    private String loadFromVault(String filePath) {
        return loadVaultProperties().getProperty(filePath);
    }

    private Properties loadVaultProperties() {
        Properties props = new Properties();
        File vaultFile   = new File(VAULT_PATH);
        if (vaultFile.exists()) {
            try (FileReader fr = new FileReader(vaultFile, StandardCharsets.UTF_8)) {
                props.load(fr);
            } catch (IOException e) {
                System.out.println("[M4] Warning: Could not read .vault: " + e.getMessage());
            }
        }
        return props;
    }
}