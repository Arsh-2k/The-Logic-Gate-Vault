// File: EventDispatcher.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
//   java -cp out MainApp

import java.io.File;
import java.util.ArrayList;

// Interface that decouples MainApp (the GUI) from FileOrchestrator (the logic).
// Without this, MainApp would need to know every detail of how encryption works.
// With this interface, MainApp calls the method — FileOrchestrator handles the rest.
// This demonstrates the INTERFACE concept and the Dependency Inversion principle.
public interface EventDispatcher {

    // Triggered when the user clicks ENCRYPT
    // filePath  = absolute path of the file to encrypt
    // password  = the password entered by the user
    // algorithm = "AES-256" or "XOR"
    void onEncrypt(String filePath, char[] password, String algorithm);

    // Triggered when the user clicks DECRYPT
    // filePath = absolute path of the .enc file to decrypt
    void onDecrypt(String filePath, char[] password, String algorithm);

    // Triggered when the user clicks Batch Encrypt
    // files = list of files to encrypt in one background operation
    void onBatchEncrypt(ArrayList<File> files, char[] password, String algorithm);

    // Triggered when the user clicks Batch Decrypt
    // files = list of .enc files to decrypt in one background operation
    void onBatchDecrypt(ArrayList<File> files, char[] password, String algorithm);

    // Triggered when the user clicks Cloud Sync / Upload
    // encryptedFilePath = path to a .enc file only (plaintext rejected at CloudSyncManager)
    void onCloudSync(String encryptedFilePath);

    // Triggered when an ADMIN clicks Key Escrow Recovery
    // Only works on AES-256 encrypted .enc files
    void onEscrowRecovery(String filePath);
}