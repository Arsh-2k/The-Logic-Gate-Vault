// File: BatchProcessor.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Part of Module M2 — Batch Processing.
// Extends SwingWorker so multi-file encryption runs on a BACKGROUND THREAD.
// Without this, encrypting many large files would freeze the entire Swing GUI
// because all Swing work happens on the Event Dispatch Thread (EDT).
//
// SwingWorker pattern:
//   doInBackground() — runs on background thread (heavy work here)
//   publish(value)   — sends progress updates to the EDT
//   process(list)    — runs on EDT, safe to update GUI components
//   done()           — runs on EDT when background work finishes
public class BatchProcessor extends SwingWorker<Void, Integer> {

    private ArrayList<File> filesToProcess; // queue of files to encrypt
    private char[]          password;       // shared password for all files
    private String          algorithm;      // "AES-256" or "XOR"
    private JProgressBar    progressBar;    // GUI progress bar to update
    private JTextArea       logTextArea;    // log panel to refresh when done
    private String          currentUser;    // username for audit logging

    public BatchProcessor(ArrayList<File> files, char[] password, String algorithm,
                          JProgressBar progressBar, JTextArea logTextArea, String currentUser) {
        this.filesToProcess = files;
        this.password       = password;
        this.algorithm      = algorithm;
        this.progressBar    = progressBar;
        this.logTextArea    = logTextArea;
        this.currentUser    = currentUser;
    }

    // Runs on the BACKGROUND THREAD — heavy encryption work goes here.
    // The GUI stays fully responsive while this executes.
    @Override
    protected Void doInBackground() throws Exception {
        int totalFiles = filesToProcess.size();

        for (int i = 0; i < totalFiles; i++) {
            File currentFile = filesToProcess.get(i);

            try {
                // Create a fresh orchestrator per batch call
                FileOrchestrator orchestrator = new FileOrchestrator(currentUser);
                orchestrator.orchestrateEncrypt(currentFile, password, algorithm);

            } catch (Exception e) {
                // Log the error but continue — one bad file should not stop the whole batch
                final String errorMsg =
                    "BATCH ERROR on '" + currentFile.getName() + "': " + e.getMessage();
                // Must use invokeLater to touch Swing components from a background thread
                SwingUtilities.invokeLater(() -> logTextArea.append(errorMsg + "\n"));
            }

            // Publish progress percentage so process() can update the progress bar
            int percent = (int) (((double)(i + 1) / totalFiles) * 100);
            publish(percent);
        }

        return null; // Void return — we don't need to return a value
    }

    // Runs on the EDT when publish() sends a value — safe to update GUI here.
    @Override
    protected void process(List<Integer> progressValues) {
        // Only care about the latest value in the batch
        int latest = progressValues.get(progressValues.size() - 1);
        progressBar.setValue(latest);
        progressBar.setString("Batch: " + latest + "%");
    }

    // Runs on the EDT when doInBackground() finishes — final GUI cleanup.
    @Override
    protected void done() {
        progressBar.setValue(100);
        progressBar.setString("Batch Complete!");

        // Refresh the log display with the latest entries
        SwingUtilities.invokeLater(() -> {
            String logContents = ActivityLogger.getInstance().getLogContents();
            logTextArea.setText(logContents);
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });

        // Memory security: clear the shared password now that all files are done.
        // char[] can be zeroed; String cannot — that's exactly why we use char[].
        Arrays.fill(password, '\0'); // memory security — zero password from RAM
    }
}
