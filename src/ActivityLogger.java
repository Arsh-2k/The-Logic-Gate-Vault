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
// Logs every encrypt/decrypt to logs/activity.log with timestamps
// Arshpreet Singh | S25CSEU0980

public class ActivityLogger {

    private final File logFile;
    private LocalDateTime timestamp;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<String> entries = new ArrayList<String>();

    public ActivityLogger() {
        new File("logs").mkdirs();
        this.logFile = new File("logs" + File.separator + "activity.log");
        loadExistingLog();
    }

    public synchronized void log(String action, String filePath, String algo,
                                  long sizeBytes, long elapsedMs) {
        timestamp = LocalDateTime.now();
        String fileName = new File(filePath).getName();

        String entry = timestamp.format(FMT) + "  [" + action + "]\n"
                + "  File:  " + fileName + "     Algo: " + algo + "\n"
                + "  Size:  " + String.format("%,d", sizeBytes)
                + " bytes   Time: " + elapsedMs + "ms\n"
                + "------------------------------------------------";

        entries.add(entry);
        writeEntry(entry);
    }

    public synchronized void logError(String context, String error) {
        timestamp = LocalDateTime.now();
        String msg = (error != null) ? error : "unknown";
        String entry = timestamp.format(FMT) + "  [ERROR]  " + context + ": " + msg;
        entries.add(entry);
        writeEntry(entry);
    }

    public synchronized void logInfo(String message) {
        timestamp = LocalDateTime.now();
        String entry = timestamp.format(FMT) + "  [INFO]   " + message;
        entries.add(entry);
        writeEntry(entry);
    }

    public List<String> getLogs() {
        return new ArrayList<String>(entries);
    }

    public String getLogText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Logic-Gate Vault Activity Log ===\n\n");
        for (String e : entries) {
            sb.append(e).append("\n\n");
        }
        return sb.toString();
    }

    public synchronized void clearLog() {
        entries.clear();
        try {
            FileWriter fw = new FileWriter(logFile, false);
            fw.close();
        } catch (IOException ignored) {}
    }

    public File getLogFile() {
        return logFile;
    }

    private void writeEntry(String entry) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            bw.write(entry);
            bw.newLine();
            bw.newLine();
            bw.close();
        } catch (IOException ex) {
            System.err.println("Logger error: " + ex.getMessage());
        }
    }

    // load previous session's log on startup
    // normalise \r\n -> \n so it works correctly on Windows
    private void loadExistingLog() {
        if (!logFile.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(logFile.toPath());
            if (bytes.length == 0) return;

            String raw = new String(bytes, StandardCharsets.UTF_8);
            raw = raw.replace("\r\n", "\n").replace("\r", "\n");

            String[] parts = raw.split("\n\n");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
        } catch (IOException ignored) {}
    }
}