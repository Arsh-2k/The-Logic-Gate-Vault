import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ActivityLogger.java
// Logs every encrypt/decrypt operation to logs/activity.log with a timestamp
// Arshpreet Singh | S25CSEU0980

public class ActivityLogger {

    private final File logFile;

    // FIX: 'timestamp' was an instance field that got reassigned inside every method.
    // That kind of shared mutable state is risky. It is now a local variable inside each
    // method that needs it, which is safer and avoids unnecessary state in the object.

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory list of log entries so the GUI can display them without re-reading the file
    private final List<String> entries = new ArrayList<>();

    public ActivityLogger() {
        // Create the logs\ folder if it does not exist yet
        new File("logs").mkdirs();
        this.logFile = new File("logs" + File.separator + "activity.log");
        loadExistingLog();
    }

    // Log one encrypt or decrypt event with full details
    // synchronized: only one thread can write to the log at a time (batch safety)
    public synchronized void log(String action, String filePath, String algo,
                                  long sizeBytes, long elapsedMs) {
        // LocalDateTime is now a local variable - no shared mutable field
        LocalDateTime timestamp = LocalDateTime.now();
        String fileName = new File(filePath).getName();

        String entry = timestamp.format(FMT) + "  [" + action + "]\n"
                + "  File:  " + fileName + "     Algo: " + algo + "\n"
                + "  Size:  " + String.format("%,d", sizeBytes)
                + " bytes   Time: " + elapsedMs + "ms\n"
                + "------------------------------------------------";

        entries.add(entry);
        writeEntry(entry);
    }

    // Log an error with context (e.g. which operation failed)
    public synchronized void logError(String context, String error) {
        LocalDateTime timestamp = LocalDateTime.now();
        String msg = (error != null) ? error : "unknown";
        String entry = timestamp.format(FMT) + "  [ERROR]  " + context + ": " + msg;
        entries.add(entry);
        writeEntry(entry);
    }

    // Log a general info or warning message
    public synchronized void logInfo(String message) {
        LocalDateTime timestamp = LocalDateTime.now();
        String entry = timestamp.format(FMT) + "  [INFO]   " + message;
        entries.add(entry);
        writeEntry(entry);
    }

    // Return a copy of all log entries (used by the GUI)
    public List<String> getLogs() {
        return new ArrayList<>(entries);
    }

    // Return the full log as one string for the JTextArea
    public String getLogText() {
        StringBuilder sb = new StringBuilder("=== Logic-Gate Vault Activity Log ===\n\n");
        for (String e : entries) {
            sb.append(e).append("\n\n");
        }
        return sb.toString();
    }

    // Wipe in-memory entries and truncate the log file on disk
    public synchronized void clearLog() {
        entries.clear();
        // Opening FileWriter with append=false truncates the file to zero bytes
        try (FileWriter fw = new FileWriter(logFile, false)) {
            // intentionally empty - just truncating
        } catch (IOException ignored) {}
    }

    public File getLogFile() {
        return logFile;
    }

    // Append one entry to the log file using try-with-resources
    // (guaranteed to close the writer even if an exception occurs)
    private void writeEntry(String entry) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            bw.write(entry);
            bw.newLine();
            bw.newLine();
        } catch (IOException ex) {
            System.err.println("Logger write error: " + ex.getMessage());
        }
    }

    // Load entries from a previous session so the log persists across restarts
    // Normalise \r\n -> \n so it parses correctly on both Windows and Linux
    private void loadExistingLog() {
        if (!logFile.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(logFile.toPath());
            if (bytes.length == 0) return;

            String raw = new String(bytes, StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");

            String[] parts = raw.split("\n\n");
            for (int i = 0; i < parts.length; i++) {
                String trimmed = parts[i].trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
        } catch (IOException ignored) {}
    }
}