import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// FileHandler.java
// Reads/writes files and handles batch processing (M2)
// Arshpreet Singh | S25CSEU0980

public class FileHandler {

    private String filePath;
    private byte[] fileBytes;

    public FileHandler() {}

    public FileHandler(String filePath) {
        this.filePath = filePath;
    }

    // read any file as raw bytes
    public byte[] readFile(String path) throws IOException {
        this.filePath = path;
        this.fileBytes = Files.readAllBytes(Paths.get(path));
        return this.fileBytes;
    }

    // write bytes to a file
    public void writeFile(String path, byte[] data) throws IOException {
        Files.write(Paths.get(path), data);
    }

    // encrypt one file, output saved as <original>.enc
    public String encryptFile(String inputPath, EncryptionEngine engine,
                              ActivityLogger logger) throws Exception {
        long start = System.currentTimeMillis();
        byte[] raw = readFile(inputPath);
        byte[] enc = engine.encrypt(raw);

        String outPath = inputPath + ".enc";
        writeFile(outPath, enc);

        long elapsed = System.currentTimeMillis() - start;
        logger.log("ENCRYPT", inputPath, engine.getAlgorithm(), raw.length, elapsed);
        return outPath;
    }

    // decrypt one file
    // removes .enc extension if present, otherwise appends _dec
    public String decryptFile(String inputPath, EncryptionEngine engine,
                              ActivityLogger logger) throws Exception {
        long start = System.currentTimeMillis();
        byte[] raw = readFile(inputPath);
        byte[] dec = engine.decrypt(raw);

        String outPath;
        if (inputPath.endsWith(".enc")) {
            outPath = inputPath.substring(0, inputPath.length() - 4);
        } else {
            int dot = inputPath.lastIndexOf('.');
            if (dot > 0) {
                outPath = inputPath.substring(0, dot) + "_dec" + inputPath.substring(dot);
            } else {
                outPath = inputPath + "_dec";
            }
        }
        writeFile(outPath, dec);

        long elapsed = System.currentTimeMillis() - start;
        logger.log("DECRYPT", inputPath, engine.getAlgorithm(), raw.length, elapsed);
        return outPath;
    }

    // encrypt multiple files using a thread pool (M2 batch)
    public List<String> batchEncrypt(List<String> filePaths, EncryptionEngine engine,
                                     ActivityLogger logger, ProgressCallback cb) {
        return doBatch(filePaths, engine, logger, cb, true);
    }

    public List<String> batchDecrypt(List<String> filePaths, EncryptionEngine engine,
                                     ActivityLogger logger, ProgressCallback cb) {
        return doBatch(filePaths, engine, logger, cb, false);
    }

    private List<String> doBatch(List<String> filePaths, final EncryptionEngine engine,
                                  final ActivityLogger logger, final ProgressCallback cb,
                                  final boolean doEncrypt) {
        List<String> results = new ArrayList<String>();
        if (filePaths.isEmpty()) return results;

        int numThreads = Math.min(filePaths.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> futures = new ArrayList<Future<String>>();

        for (final String path : filePaths) {
            Future<String> future = pool.submit(new java.util.concurrent.Callable<String>() {
                public String call() {
                    String name = Paths.get(path).getFileName().toString();
                    try {
                        if (doEncrypt) {
                            encryptFile(path, engine, logger);
                        } else {
                            decryptFile(path, engine, logger);
                        }
                        return "[OK]   " + name + (doEncrypt ? " -> .enc" : " -> decrypted");
                    } catch (Exception ex) {
                        return "[ERR]  " + name + ": " + ex.getMessage();
                    }
                }
            });
            futures.add(future);
        }

        pool.shutdown();

        int done = 0;
        for (Future<String> f : futures) {
            String res;
            try {
                res = f.get();
            } catch (Exception ex) {
                res = "[ERR]  " + ex.getMessage();
            }
            done++;
            results.add(res);
            if (cb != null) {
                cb.onProgress(done, filePaths.size(), res);
            }
        }
        return results;
    }

    public String getFilePath() { return filePath; }
    public byte[] getFileBytes() { return fileBytes; }

    public interface ProgressCallback {
        void onProgress(int done, int total, String lastResult);
    }
}