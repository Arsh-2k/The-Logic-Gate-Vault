// File: FileHandler.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

import java.io.*;

// Handles all file read/write operations for P2 (Encrypt/Decrypt process).
// Uses standard FileInputStream and FileOutputStream.
//
// ATOMIC WRITE SAFETY:
//   We write to filePath.tmp first, then rename to the final name.
//   If the write fails halfway, the original file is never touched.
//
// FIX APPLIED (Point 4): All streams now use try-with-resources.
// This guarantees streams are closed even if an exception occurs mid-write,
// preventing locked file handles on Windows (a common issue on Windows OS).
public class FileHandler {

    // Read ALL bytes from a file and return them as a byte array.
    // Uses readAllBytes() — guaranteed to read the entire file in one call.
    // FIX (Point 4): FileInputStream now in try-with-resources block
    public byte[] readFile(String filePath) throws Exception {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }
        if (!file.canRead()) {
            throw new Exception(
                "Cannot read file (may be locked by another program): " + filePath);
        }

        // try-with-resources — fis is automatically closed after the block
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes(); // guaranteed complete read
        }
        // NOTE on large files: readAllBytes() loads the entire file into a byte[].
        // This works correctly for files up to ~256MB depending on JVM heap settings.
        // For very large files (2GB+), a streaming approach using CipherInputStream
        // would be required — this is listed as a future scope item in the report
        // (Section 9) as it would require changing the EncryptionEngine interface
        // signature from byte[] to File-in/File-out.
    }

    // Write bytes to disk SAFELY using an atomic temp-file rename.
    // Step 1: write all bytes to filePath.tmp
    // Step 2: rename .tmp → final name (atomic on most file systems)
    // FIX (Point 4): FileOutputStream now in try-with-resources block
    public void writeFile(String filePath, byte[] data) throws Exception {
        File finalFile = new File(filePath);
        File tempFile  = new File(filePath + ".tmp");

        // try-with-resources — fos is automatically closed after the block
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
            fos.flush(); // push all bytes to the OS write buffer
        }
        // NOTE: if the write fails above, tempFile may be partial,
        // but finalFile is untouched — the original data is safe.

        if (finalFile.exists()) {
            finalFile.delete();
        }

        boolean renamed = tempFile.renameTo(finalFile);
        if (!renamed) {
            throw new Exception("Could not save output file: " + filePath);
        }
    }

    // Delete a file. Returns true on success.
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}