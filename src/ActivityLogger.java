// File: ActivityLogger.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
//   java -cp out MainApp

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// P3 — Audit Log process.
// Data Store D3 = logs/activity.log (DFD requirement).
//
// Implements the SINGLETON pattern — only ONE instance can ever exist.
//   • Private constructor prevents external instantiation.
//   • Double-checked locking with a volatile field for thread safety.
//   • Single point of truth for all audit events across all threads.
//
// ALL user and admin activity is routed through this class so that
// the Admin's right-panel monitoring view shows a complete picture:
//   • File operations : log()
//   • Login events    : logEvent()   ← NEW in v5
//   • Errors / faults : logError()
public class ActivityLogger {

    // D3 data store path — matches DFD and directory structure specification
    public static final String LOG_DIR  = "logs";
    public static final String LOG_PATH = "logs/activity.log";

    // volatile guarantees that all threads see the latest reference immediately.
    // Without volatile, a second thread could see a partially-constructed object.
    private static volatile ActivityLogger instance = null;

    private final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // PRIVATE constructor — no other class can call 'new ActivityLogger()'
    // This is the cornerstone of the Singleton pattern.
    //
    // FIX 1 — Log Erasure on Restart:
    //   The previous implementation called writeToFile(..., false) unconditionally,
    //   which truncated the log file every time the JVM started.  This violated the
    //   requirement that audit entries persist across sessions.
    //
    //   New behaviour:
    //     • First launch (file absent or empty) → write the "Activity Log" header
    //       with append=false so a clean file is created exactly once.
    //     • Subsequent launches (file exists and has content) → write only a short
    //       session-boundary marker with append=true, preserving all prior entries.
    private ActivityLogger() {
        new File(LOG_DIR).mkdirs(); // create logs/ directory if it doesn't exist

        File logFile = new File(LOG_PATH);
        boolean logExistsWithContent = logFile.exists() && logFile.length() > 0;

        if (logExistsWithContent) {
            // Existing log — append a session-boundary marker so the Admin can
            // clearly see where one JVM session ends and the next begins.
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writeToFile("\n=== Session Started: " + timestamp + " ===\n", true);
        } else {
            // First launch or empty file — write the standard header (overwrite).
            writeToFile("=== Logic-Gate Vault Activity Log ===\n", false);
        }
    }

    // Double-checked locking Singleton — thread-safe and efficient.
    // The first null check avoids synchronisation on the common (post-init) path.
    // The second null check inside the synchronized block prevents a race where
    // two threads both pass the outer check simultaneously.
    public static ActivityLogger getInstance() {
        if (instance == null) {
            synchronized (ActivityLogger.class) {
                if (instance == null) {
                    instance = new ActivityLogger();
                }
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Log a completed file operation (encrypt / decrypt / batch / sync).
    // Format matches the activity log screenshots in the project PPT.
    // -----------------------------------------------------------------------
    public void log(String username, String operation, String fileName,
                    String algorithm, long fileSizeBytes, long durationMs) {
        String timestamp = LocalDateTime.now().format(formatter);
        String entry = timestamp + " [" + operation + "]\n"
                + "  User: "   + username    + "\n"
                + "  File: "   + fileName    + "    Algo: " + algorithm + "\n"
                + "  Size: "   + fileSizeBytes + " bytes"
                + "    Time: " + durationMs  + "ms\n"
                + "------------------------------------------\n";
        writeToFile(entry, true);
    }

    // -----------------------------------------------------------------------
    // Log a named authentication / session event.
    // NEW in v5 — required so that login successes and failures appear in the
    // Admin's monitoring view alongside file-operation entries.
    //
    // eventType examples: LOGIN_SUCCESS, LOGIN_FAILED, SESSION_TIMEOUT
    // detail   : optional free-text context (trimmed; omitted when empty)
    // -----------------------------------------------------------------------
    public void logEvent(String username, String eventType, String detail) {
        String timestamp = LocalDateTime.now().format(formatter);
        String suffix    = (detail != null && !detail.trim().isEmpty())
                           ? " — " + detail.trim() : "";
        String entry = timestamp + " [" + eventType + "] User: " + username + suffix + "\n";
        writeToFile(entry, true);
    }

    // -----------------------------------------------------------------------
    // Log an error / warning event (wrong password, OOM, corrupted file, etc.)
    // -----------------------------------------------------------------------
    public void logError(String username, String errorMessage) {
        String timestamp = LocalDateTime.now().format(formatter);
        String entry = timestamp + " [ERROR] User: " + username
                + " — " + errorMessage + "\n";
        writeToFile(entry, true);
    }

    // -----------------------------------------------------------------------
    // Read the entire log and return as a String for the right-panel GUI view.
    // Admin monitoring calls this on every refresh to show latest activity.
    // -----------------------------------------------------------------------
    public String getLogContents() {
        File logFile = new File(LOG_PATH);
        if (!logFile.exists()) {
            return "No activity log found yet. Perform an operation to begin logging.";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new FileReader(logFile, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Could not read activity log: " + e.getMessage();
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Clear the log file — Admin-only, wired to the "Clear Activity Log" button.
    // -----------------------------------------------------------------------
    public void clearLog() {
        writeToFile("=== Logic-Gate Vault Activity Log (Cleared) ===\n", false);
    }

    // -----------------------------------------------------------------------
    // Internal write helper.
    // append=true  → add to end of file (normal log append)
    // append=false → overwrite (used by constructor and clearLog)
    //
    // FIX 2 — Thread Safety Hazard:
    //   The previous implementation had no synchronisation.  The Swing EDT
    //   (encrypt/decrypt buttons) and a background SwingWorker thread
    //   (BatchProcessor) can both call log() simultaneously.  On Windows,
    //   two threads opening the same FileWriter concurrently produces an
    //   IOException: "The process cannot access the file because it is being
    //   used by another process."
    //
    //   Adding 'synchronized' serialises all write calls on the ActivityLogger
    //   instance monitor.  Because there is only ever ONE ActivityLogger (Singleton),
    //   this monitor is the single shared lock for the entire application.
    //   The EDT and every SwingWorker thread will queue their writes safely.
    //
    //   try-with-resources still guarantees FileWriter is closed even on exception,
    //   preventing locked file handles on Windows regardless of concurrency.
    // -----------------------------------------------------------------------
    private synchronized void writeToFile(String content, boolean append) {
        try (FileWriter fw = new FileWriter(LOG_PATH, append)) {
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            // Logging must never crash the application — print a console warning only.
            System.out.println("Warning: Could not write to activity log: " + e.getMessage());
        }
    }
}