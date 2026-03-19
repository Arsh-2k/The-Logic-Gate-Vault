import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// FileHandler.java
// Reads and writes files, handles single file and batch operations
// Arshpreet Singh | S25CSEU0980

public class FileHandler {

    // NOTE: filePath and fileBytes fields were kept to match the class diagram.
    // getFilePath() and getFileBytes() were removed because they were declared
    // but never called from outside this class - that causes yellow warnings in VS Code.
    private String filePath;
    private byte[] fileBytes;

    public FileHandler() {}

    // Read any file as a raw byte array - works for all file types
    public byte[] readFile(String path) throws IOException {
        this.filePath  = path;
        this.fileBytes = Files.readAllBytes(Paths.get(path));
        return this.fileBytes;
    }

    // Write a byte array to a file at the given path
    public void writeFile(String path, byte[] data) throws IOException {
        Files.write(Paths.get(path), data);
    }

    // -----------------------------------------------------------------------
    // Encrypt one file
    // Steps:
    //   1. Read the original file into a byte array
    //   2. Encrypt those bytes
    //   3. FIX: Check if the .enc output file already exists - warn user if so
    //   4. Write the encrypted bytes to <originalName>.enc
    //   5. Delete the original file (only after .enc is safely written)
    // -----------------------------------------------------------------------
    public String encryptFile(String inputPath, EncryptionEngine engine,
                              ActivityLogger logger) throws Exception {

        long start = System.currentTimeMillis();

        // Read the original file
        byte[] raw = readFile(inputPath);

        // Encrypt the bytes
        byte[] enc = engine.encrypt(raw);

        // FIX: Check if the output .enc file already exists before writing.
        // Without this check, Files.write() silently overwrites the old .enc file,
        // which could mean the user loses a previously encrypted backup.
        String outPath = inputPath + ".enc";
        File outFile = new File(outPath);
        if (outFile.exists()) {
            throw new IOException(
                "Output file already exists: " + outFile.getName()
                + "\nDelete or rename it first, then try again.");
        }

        // Write the encrypted bytes to the .enc file
        writeFile(outPath, enc);

        // Delete the original file now that the .enc is fully written.
        // We delete AFTER writing to avoid data loss if the write fails.
        File original = new File(inputPath);
        boolean deleted = original.delete();

        if (!deleted) {
            // The encryption worked - just warn that manual cleanup is needed
            logger.logInfo("Warning: could not delete original file: " + original.getName());
        }

        long elapsed = System.currentTimeMillis() - start;
        logger.log("ENCRYPT", inputPath, engine.getAlgorithm(), raw.length, elapsed);
        return outPath;
    }

    // -----------------------------------------------------------------------
    // Decrypt one file
    // Steps:
    //   1. Read the .enc file into a byte array
    //   2. Decrypt those bytes
    //   3. Work out the output file name (remove .enc extension)
    //   4. FIX: Check if the output file already exists
    //   5. Write the decrypted bytes to the restored file
    //   6. Delete the .enc file (only after decrypted file is safely written)
    // -----------------------------------------------------------------------
    public String decryptFile(String inputPath, EncryptionEngine engine,
                              ActivityLogger logger) throws Exception {

        long start = System.currentTimeMillis();

        // Read the encrypted file
        byte[] raw = readFile(inputPath);

        // Decrypt the bytes
        byte[] dec = engine.decrypt(raw);

        // Work out the output file name
        // If it ends in .enc, remove that to restore the original name
        // e.g. notes.txt.enc -> notes.txt
        String outPath;
        if (inputPath.endsWith(".enc")) {
            outPath = inputPath.substring(0, inputPath.length() - 4);
        } else {
            // Fallback: add _dec before the file extension
            int dot = inputPath.lastIndexOf('.');
            if (dot > 0) {
                outPath = inputPath.substring(0, dot) + "_dec" + inputPath.substring(dot);
            } else {
                outPath = inputPath + "_dec";
            }
        }

        // FIX: Check if the restored file already exists before writing.
        // This prevents silently overwriting an existing file with the same name.
        File outFile = new File(outPath);
        if (outFile.exists()) {
            throw new IOException(
                "Output file already exists: " + outFile.getName()
                + "\nDelete or rename it first, then try again.");
        }

        // Write the decrypted bytes to the restored file
        writeFile(outPath, dec);

        // Delete the .enc file now that decryption is done.
        // We delete AFTER writing to avoid losing data if the write fails.
        File encFile = new File(inputPath);
        boolean deleted = encFile.delete();

        if (!deleted) {
            logger.logInfo("Warning: could not delete encrypted file: " + encFile.getName());
        }

        long elapsed = System.currentTimeMillis() - start;
        logger.log("DECRYPT", inputPath, engine.getAlgorithm(), raw.length, elapsed);
        return outPath;
    }

    // -----------------------------------------------------------------------
    // Batch encrypt - encrypts every file in the list one by one
    // -----------------------------------------------------------------------
    // Uses a plain for loop so the logic is easy to follow and explain.
    // Each file is processed completely (encrypt + delete original) before
    // the loop moves on to the next one.
    // If one file fails, its error is recorded and the loop continues.
    // File.delete() is INSIDE the loop (via encryptFile) so it runs for every file.
    // -----------------------------------------------------------------------
    public List<String> batchEncrypt(List<String> filePaths, EncryptionEngine engine,
                                     ActivityLogger logger, ProgressCallback cb) {

        List<String> results = new ArrayList<>();

        for (int i = 0; i < filePaths.size(); i++) {

            String path = filePaths.get(i);
            String name = new File(path).getName();

            try {
                // Encrypt this file (original is deleted inside encryptFile)
                encryptFile(path, engine, logger);
                results.add("[OK]   " + name + " -> .enc");

            } catch (Exception ex) {
                // Record the error for this file and continue with the next one
                results.add("[ERR]  " + name + ": " + ex.getMessage());
            }

            // Notify the GUI of progress after each file
            if (cb != null) {
                cb.onProgress(i + 1, filePaths.size(), results.get(i));
            }
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Batch decrypt - same structure as batchEncrypt
    // File.delete() is INSIDE the loop (via decryptFile) so every .enc file
    // gets cleaned up, not just the last one.
    // -----------------------------------------------------------------------
    public List<String> batchDecrypt(List<String> filePaths, EncryptionEngine engine,
                                     ActivityLogger logger, ProgressCallback cb) {

        List<String> results = new ArrayList<>();

        for (int i = 0; i < filePaths.size(); i++) {

            String path = filePaths.get(i);
            String name = new File(path).getName();

            try {
                // Decrypt this file (.enc file is deleted inside decryptFile)
                decryptFile(path, engine, logger);
                results.add("[OK]   " + name + " -> decrypted");

            } catch (Exception ex) {
                results.add("[ERR]  " + name + ": " + ex.getMessage());
            }

            if (cb != null) {
                cb.onProgress(i + 1, filePaths.size(), results.get(i));
            }
        }

        return results;
    }

    // Callback interface so the GUI can receive progress updates during a batch
    public interface ProgressCallback {
        void onProgress(int done, int total, String lastResult);
    }
}