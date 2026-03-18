import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// MainApp.java - Entry point for The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | SCSET, Bennett University

// HOW TO RUN:
//   1. Open terminal in VS Code  (Ctrl + `)
//   2. mkdir out
//   3. mkdir logs
//   4. javac -d out src\*.java
//   5. java -cp out MainApp

public class MainApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // default look and feel is fine
        }

        // Swing must run on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GUIDashboard().launchGUI();
            }
        });
    }
}