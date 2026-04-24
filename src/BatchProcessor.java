// File: BatchProcessor.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
//   java -cp out MainApp

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Part of Module M2 — Batch Processing.
// Extends SwingWorker so multi-file operations run on a BACKGROUND THREAD.
// Without this, encrypting or decrypting many large files would freeze the
// entire Swing GUI because all Swing work happens on the Event Dispatch Thread (EDT).
//
// The isEncrypt flag controls whether each file is encrypted or decrypted.
// Both operations reuse the same SwingWorker machinery — only the call inside
// the loop changes. This avoids duplicating all the thread/progress-bar logic.
//
// SwingWorker pattern:
//   doInBackground() — runs on background thread (heavy work here)
//   publish(value)   — sends progress updates to the EDT
//   process(list)    — runs on EDT, safe to update GUI components
//   done()           — runs on EDT when background work finishes
public class BatchProcessor extends SwingWorker<Void, Integer> {

    private ArrayList<File> filesToProcess; // queue of files to process
    private char[]          password;       // shared password for all files
    private String          algorithm;      // "AES-256" or "XOR"
    private JProgressBar    progressBar;    // GUI progress bar to update
    private JTextArea       logTextArea;    // log panel to refresh when done
    private String          currentUser;    // username for audit logging
    private boolean         isEncrypt;      // true = encrypt, false = decrypt

    // isEncrypt = true  → each file is passed to orchestrator.orchestrateEncrypt()
    // isEncrypt = false → each file is passed to orchestrator.orchestrateDecrypt()
    public BatchProcessor(ArrayList<File> files, char[] password, String algorithm,
                          JProgressBar progressBar, JTextArea logTextArea,
                          String currentUser, boolean isEncrypt) {
        this.filesToProcess = files;
        this.password       = password;
        this.algorithm      = algorithm;
        this.progressBar    = progressBar;
        this.logTextArea    = logTextArea;
        this.currentUser    = currentUser;
        this.isEncrypt      = isEncrypt;
    }

    // Runs on the BACKGROUND THREAD — heavy file work goes here.
    // The GUI stays fully responsive while this executes.
    @Override
    protected Void doInBackground() throws Exception {
        int totalFiles = filesToProcess.size();

        for (int i = 0; i < totalFiles; i++) {
            File currentFile = filesToProcess.get(i);

            try {
                // Create a fresh orchestrator per batch call so there is no
                // shared mutable state between iterations.
                FileOrchestrator orchestrator = new FileOrchestrator(currentUser);

                if (isEncrypt) {
                    orchestrator.orchestrateEncrypt(currentFile, password, algorithm);
                } else {
                    orchestrator.orchestrateDecrypt(currentFile, password, algorithm);
                }

            } catch (Exception e) {
                // Log the error and continue — one bad file must not stop the whole batch.
                // Must use invokeLater to touch Swing components from a background thread.
                final String errorMsg =
                    "BATCH ERROR on '" + currentFile.getName() + "': " + e.getMessage();
                SwingUtilities.invokeLater(() -> logTextArea.append(errorMsg + "\n"));
            }

            // Publish progress percentage so process() can update the bar on the EDT.
            int percent = (int) (((double)(i + 1) / totalFiles) * 100);
            publish(percent);
        }

        return null; // Void return — the result is the side-effect on disk
    }

    // Runs on the EDT when publish() sends a value — safe to update GUI here.
    @Override
    protected void process(List<Integer> progressValues) {
        // We only care about the latest value — earlier ones may have been
        // batched together by SwingWorker if the loop ran faster than the EDT.
        int latest = progressValues.get(progressValues.size() - 1);
        progressBar.setValue(latest);
        // Dynamic label so the user knows whether encrypt or decrypt is running.
        progressBar.setString(
            (isEncrypt ? "Batch Encrypting" : "Batch Decrypting") + "... " + latest + "%");
    }

    // Runs on the EDT when doInBackground() finishes — final GUI cleanup.
    @Override
    protected void done() {
        progressBar.setValue(100);
        progressBar.setString(
            isEncrypt ? "Batch Encrypt Complete!" : "Batch Decrypt Complete!");

        // Refresh the log panel with the latest entries written during the batch.
        SwingUtilities.invokeLater(() -> {
            String logContents = ActivityLogger.getInstance().getLogContents();
            logTextArea.setText(logContents);
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });

        // Memory security: zero the shared password now that all files are done.
        // char[] can be explicitly cleared; String literals cannot — that is why
        // passwords are always stored as char[] throughout this application.
        Arrays.fill(password, '\0'); // memory security — zero password from RAM
    }
}