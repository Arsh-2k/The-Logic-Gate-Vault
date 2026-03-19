import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// MainApp.java
// Entry point for The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -d out src\*.java
//   java -cp out MainApp

public class MainApp {

    public static void main(String[] args) {

        // Use the Windows native look and feel so buttons and panels render cleanly
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException e) {
            // If it fails, Java's default look and feel is used - still works fine
        }

        // Swing must always be started on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new GUIDashboard().launchGUI());
    }
}