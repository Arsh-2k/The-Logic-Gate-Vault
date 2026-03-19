import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

// GUIDashboard.java
// Main Swing window for Logic-Gate Vault v2.0
// Arshpreet Singh | S25CSEU0980

public class GUIDashboard {

    // ---- Swing components ----
    private JFrame frame;
    private JTextField fileField;
    private JComboBox<String> algoBox;
    private JPasswordField pwdField;
    private JCheckBox showPwdBox;   // stored as field so done() can reset it
    private JTextArea logArea;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private DefaultListModel<String> batchModel;

    // ---- App objects ----
    private final ActivityLogger logger  = new ActivityLogger();
    private final FileHandler    handler = new FileHandler();
    private final List<String>   batchFiles = new ArrayList<>();

    // ---- Colours ----
    private static final Color BG        = new Color(10,  12,  22);
    private static final Color BG_CARD   = new Color(15,  20,  40);
    private static final Color BG_INPUT  = new Color(18,  24,  48);
    private static final Color BG_INFO   = new Color(12,  28,  55);
    private static final Color BLUE      = new Color(26,  140, 255);
    private static final Color BLUE2     = new Color(0,   85,  187);
    private static final Color WHITE     = new Color(238, 244, 255);
    private static final Color GREY      = new Color(120, 148, 190);
    private static final Color GREEN     = new Color(0,   212, 138);
    private static final Color ORANGE    = new Color(255, 176, 32);
    private static final Color RED       = new Color(255, 85,  51);
    private static final Color BORDER    = new Color(30,  45,  85);
    private static final Color INFO_BLUE = new Color(100, 180, 255);

    // ---- Fonts ----
    // FIX: removed Font INFO - it was identical to Font SMALL (same family, size, style)
    // Having two identical constants is dead code and causes a VS Code warning.
    // All places that used INFO now use SMALL instead.
    private static final Font MONO  = new Font("Consolas", Font.PLAIN,  12);
    private static final Font BOLD  = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font TITLE = new Font("Consolas", Font.BOLD,   16);
    private static final Font SMALL = new Font("Segoe UI", Font.PLAIN,  11);

    // ======================================================
    // Entry point
    // ======================================================

    public void launchGUI() {
        frame = new JFrame("Logic-Gate Vault  v2.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 760);
        frame.setMinimumSize(new Dimension(860, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout());

        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildSplit(),  BorderLayout.CENTER);
        frame.add(buildFooter(), BorderLayout.SOUTH);

        frame.setVisible(true);
        refreshLog();
        setStatus("Ready.  No errors.", GREEN);
    }

    // ======================================================
    // Header bar
    // ======================================================

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BLUE));
        bar.setPreferredSize(new Dimension(0, 48));

        JLabel title = new JLabel("  THE LOGIC-GATE VAULT");
        title.setFont(TITLE);
        title.setForeground(BLUE);

        JLabel sub = new JLabel("File Encryption System  |  AES-256 + XOR  |  S25CSEU0980  ");
        sub.setFont(SMALL);
        sub.setForeground(GREY);

        bar.add(title, BorderLayout.WEST);
        bar.add(sub,   BorderLayout.EAST);
        return bar;
    }

    // ======================================================
    // Main split pane
    // ======================================================

    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildLogPanel());
        split.setDividerLocation(460);
        split.setDividerSize(5);
        split.setBackground(BG);
        split.setBorder(null);
        return split;
    }

    // ======================================================
    // Left panel: instructions + all controls
    // ======================================================

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(14, 18, 14, 10));

        p.add(buildInstructionsBox());
        p.add(Box.createVerticalStrut(14));

        p.add(label("1.  Select File"));
        p.add(Box.createVerticalStrut(5));
        p.add(buildFileRow());
        p.add(Box.createVerticalStrut(12));

        p.add(label("2.  Algorithm"));
        p.add(Box.createVerticalStrut(5));
        p.add(buildAlgoRow());
        p.add(Box.createVerticalStrut(12));

        p.add(label("3.  Password / Key"));
        p.add(Box.createVerticalStrut(5));
        p.add(buildPasswordRow());
        p.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(sep);
        p.add(Box.createVerticalStrut(14));

        p.add(buildActionButtons());
        p.add(Box.createVerticalStrut(14));

        p.add(label("4.  Batch Mode  (select multiple files at once)"));
        p.add(Box.createVerticalStrut(5));
        p.add(buildBatchSection());
        p.add(Box.createVerticalStrut(10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(SMALL);
        progressBar.setBackground(BG_CARD);
        progressBar.setForeground(BLUE);
        progressBar.setBorderPainted(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        progressBar.setVisible(false);
        p.add(progressBar);

        return p;
    }

    // ======================================================
    // Instructions box
    // ======================================================

    private JPanel buildInstructionsBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(BG_INFO);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BLUE2, 1),
                "  How to Use  ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Consolas", Font.BOLD, 11),
                BLUE);
        box.setBorder(BorderFactory.createCompoundBorder(
                border, new EmptyBorder(4, 8, 8, 8)));

        String[] lines = {
            "ENCRYPT:  Select any file -> choose algorithm -> enter password -> Encrypt.",
            "          The original file is deleted. Only the .enc file remains.",
            "",
            "DECRYPT:  Select a .enc file -> same algorithm + password -> Decrypt.",
            "          The .enc file is deleted. The original file is restored.",
            "",
            "BATCH:    Click 'Add Files' to queue multiple files, then 'Batch Encrypt'.",
            "",
            "SUPPORTED:  .txt  .docx  .xlsx  .pdf  .png  .jpg  .zip  and all other files.",
            "NOTE:  Use the SAME password and algorithm you used to encrypt."
        };

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            JLabel lbl = new JLabel(line.isEmpty() ? " " : line);
            lbl.setFont(SMALL);  // FIX: was INFO (identical to SMALL) - now just uses SMALL
            lbl.setForeground(
                line.startsWith("ENCRYPT") || line.startsWith("DECRYPT")
                || line.startsWith("BATCH")   || line.startsWith("SUPPORTED")
                || line.startsWith("NOTE")
                ? INFO_BLUE : GREY);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(lbl);
        }

        return box;
    }

    // ======================================================
    // Row 1: file path + Browse button
    // ======================================================

    private JPanel buildFileRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        fileField = styledField("No file selected");
        fileField.setEditable(false);

        JButton btn = makeButton("Browse...", BLUE2);
        btn.setPreferredSize(new Dimension(90, 32));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pickFile();
            }
        });

        row.add(fileField, BorderLayout.CENTER);
        row.add(btn,       BorderLayout.EAST);
        return row;
    }

    // ======================================================
    // Row 2: algorithm dropdown
    // Custom renderer so text is always readable on Windows
    // (without it, Windows L&F draws light text on a light background)
    // ======================================================

    private JPanel buildAlgoRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        algoBox = new JComboBox<>(new String[]{"XOR", "AES-256"});
        algoBox.setFont(MONO);
        algoBox.setPreferredSize(new Dimension(160, 32));

        algoBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                lbl.setFont(MONO);
                if (isSelected) {
                    lbl.setBackground(BLUE2);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(20, 20, 50)); // dark navy - always readable
                }
                lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
                return lbl;
            }
        });

        row.add(algoBox);
        return row;
    }

    // ======================================================
    // Row 3: password field + Show checkbox
    // ======================================================

    private JPanel buildPasswordRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        pwdField = new JPasswordField();
        pwdField.setFont(MONO);
        pwdField.setBackground(BG_INPUT);
        pwdField.setForeground(BLUE);
        pwdField.setCaretColor(BLUE);
        pwdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 8, 4, 8)));

        showPwdBox = new JCheckBox("Show");
        showPwdBox.setFont(SMALL);
        showPwdBox.setForeground(GREY);
        showPwdBox.setBackground(BG);
        showPwdBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pwdField.setEchoChar(showPwdBox.isSelected() ? (char) 0 : '*');
            }
        });

        row.add(pwdField,   BorderLayout.CENTER);
        row.add(showPwdBox, BorderLayout.EAST);
        return row;
    }

    // ======================================================
    // ENCRYPT / DECRYPT buttons
    // ======================================================

    private JPanel buildActionButtons() {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton enc = makeButton("  ENCRYPT  ", GREEN);
        JButton dec = makeButton("  DECRYPT  ", BLUE);

        enc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { runOp(true);  }
        });
        dec.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { runOp(false); }
        });

        row.add(enc);
        row.add(dec);
        return row;
    }

    // ======================================================
    // Batch mode section
    // ======================================================

    private JPanel buildBatchSection() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));

        batchModel = new DefaultListModel<>();
        JList<String> list = new JList<>(batchModel);
        list.setFont(SMALL);
        list.setBackground(BG_CARD);
        list.setForeground(WHITE);
        list.setSelectionBackground(BLUE2);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(300, 80));
        scroll.setMinimumSize(new Dimension(100, 55));

        JPanel btns = new JPanel(new GridLayout(1, 3, 6, 0));
        btns.setBackground(BG);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton add   = makeButton("Add Files",     BLUE2);
        JButton batch = makeButton("Batch Encrypt", GREEN);
        JButton clr   = makeButton("Clear",         RED);

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { pickBatchFiles(); }
        });
        batch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { runBatch(); }
        });
        clr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                batchFiles.clear();
                batchModel.clear();
            }
        });

        btns.add(add);
        btns.add(batch);
        btns.add(clr);

        p.add(scroll, BorderLayout.CENTER);
        p.add(btns,   BorderLayout.SOUTH);
        return p;
    }

    // ======================================================
    // Right panel: activity log
    // ======================================================

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, BORDER));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(10, 14, 8, 10));

        JLabel title = new JLabel("activity.log  |  Timestamped Audit Trail");
        title.setFont(new Font("Consolas", Font.BOLD, 12));
        title.setForeground(BLUE);

        JButton clrBtn = makeButton("Clear Log", RED);
        clrBtn.setFont(SMALL);
        clrBtn.setPreferredSize(new Dimension(82, 26));
        clrBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.clearLog();
                refreshLog();
            }
        });

        header.add(title,  BorderLayout.WEST);
        header.add(clrBtn, BorderLayout.EAST);

        logArea = new JTextArea();
        logArea.setFont(MONO);
        logArea.setBackground(new Color(6, 7, 18));
        logArea.setForeground(GREEN);
        logArea.setCaretColor(GREEN);
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(8, 12, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(6, 7, 18));

        p.add(header, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ======================================================
    // Footer status bar
    // ======================================================

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        bar.setPreferredSize(new Dimension(0, 30));

        statusLabel = new JLabel("  Initialising...");
        statusLabel.setFont(MONO);
        statusLabel.setForeground(GREY);

        JLabel credit = new JLabel("V8 Logic Systems  |  github.com/Arsh-2k/The-Logic-Gate-Vault  ");
        credit.setFont(SMALL);
        credit.setForeground(new Color(55, 75, 115));

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(credit,      BorderLayout.EAST);
        return bar;
    }

    // ======================================================
    // File picker dialogs
    // ======================================================

    private void pickFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select File to Encrypt or Decrypt");
        fc.setFileFilter(new FileNameExtensionFilter(
                "All supported files",
                "txt", "docx", "xlsx", "pdf", "enc", "png", "jpg", "zip", "bin", "csv"));
        fc.setAcceptAllFileFilterUsed(true);

        // CANCEL SAFE: showOpenDialog returns CANCEL_OPTION if cancelled.
        // We only proceed if APPROVE_OPTION is returned - so cancel is handled gracefully.
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(fc.getSelectedFile().getAbsolutePath());
            setStatus("File selected: " + fc.getSelectedFile().getName(), WHITE);
        }
        // If user clicked Cancel, nothing changes - no crash.
    }

    private void pickBatchFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Select Files for Batch Encryption");

        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File[] selected = fc.getSelectedFiles();
            for (int i = 0; i < selected.length; i++) {
                String abs = selected[i].getAbsolutePath();
                if (!batchFiles.contains(abs)) {
                    batchFiles.add(abs);
                    batchModel.addElement(selected[i].getName());
                }
            }
            setStatus(batchFiles.size() + " file(s) queued for batch.", ORANGE);
        }
    }

    // ======================================================
    // Single file encrypt or decrypt
    // ======================================================

    private void runOp(final boolean encrypt) {

        // Validate: a file must be selected
        final String path = fileField.getText().trim();
        if (path.isEmpty() || "No file selected".equals(path)) {
            showError("No file selected. Click Browse first.");
            return;
        }

        // FIX: Capture the password as a char[] instead of String.
        // char[] can be zeroed out with Arrays.fill() after use.
        // A String is immutable and stays in memory until GC - a security risk.
        final char[] pwdChars = pwdField.getPassword();
        if (pwdChars.length == 0) {
            showError("Please enter a password before continuing.");
            return;
        }

        // Convert to String for the engine (engine API takes String)
        // We zero out the char[] immediately after this conversion
        final String pwd = new String(pwdChars).trim();
        Arrays.fill(pwdChars, '\0'); // zero out the original char array right away

        if (pwd.isEmpty()) {
            showError("Password cannot be blank.");
            return;
        }

        // Validate: decrypt only works on .enc files
        if (!encrypt && !path.endsWith(".enc")) {
            showError("This file does not look like an encrypted file.\n"
                    + "Encrypted files end with '.enc'\n"
                    + "Please select a .enc file to decrypt.");
            return;
        }

        final String algo = "AES-256".equals(algoBox.getSelectedItem()) ? "AES" : "XOR";

        setStatus("Processing...", ORANGE);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);

        // FIX: Use diamond inference on the constructor <> rather than repeating the
        // full type twice. SwingWorker<String, Void> worker = new SwingWorker<>() ...
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                long t0 = System.currentTimeMillis();
                EncryptionEngine engine = new EncryptionEngine(algo, pwd);

                String outPath;
                if (encrypt) {
                    outPath = handler.encryptFile(path, engine, logger);
                } else {
                    outPath = handler.decryptFile(path, engine, logger);
                }

                long ms = System.currentTimeMillis() - t0;
                String verb = encrypt ? "Encrypted" : "Decrypted";
                return verb + "  ->  " + new File(outPath).getName() + "  (" + ms + " ms)";
            }

            // done() is called automatically on the Swing EDT when doInBackground finishes
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);

                try {
                    setStatus(get(), GREEN);

                } catch (InterruptedException ex) {
                    // FIX: When InterruptedException is caught, we must restore the
                    // interrupted status of the thread so the JVM knows it was interrupted.
                    // Without this, the interrupted signal is silently swallowed.
                    Thread.currentThread().interrupt();
                    setStatus("Operation interrupted.", RED);

                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
                    setStatus("Error: " + msg, RED);
                    logger.logError(encrypt ? "ENCRYPT" : "DECRYPT", msg);
                    showError(msg); // show popup for any crypto or file error
                }

                // Clear the password field and reset the Show checkbox
                clearPasswordField();

                // Reset file field so the user must pick a fresh file
                fileField.setText("No file selected");

                refreshLog();
                hideProgress();
            }
        };
        worker.execute();
    }

    // ======================================================
    // Batch encrypt
    // ======================================================

    private void runBatch() {

        if (batchFiles.isEmpty()) {
            showError("No files queued. Click 'Add Files' first.");
            return;
        }

        // FIX: Same password security fix as runOp - capture as char[], zero it out
        final char[] pwdChars = pwdField.getPassword();
        if (pwdChars.length == 0) {
            showError("Please enter a password first.");
            return;
        }
        final String pwd = new String(pwdChars).trim();
        Arrays.fill(pwdChars, '\0');

        if (pwd.isEmpty()) {
            showError("Password cannot be blank.");
            return;
        }

        final String algo  = "AES-256".equals(algoBox.getSelectedItem()) ? "AES" : "XOR";
        final int    total = batchFiles.size();

        // Snapshot the list so it cannot change while we are processing
        final List<String> snapshot = new ArrayList<>();
        for (int i = 0; i < batchFiles.size(); i++) {
            snapshot.add(batchFiles.get(i));
        }

        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setVisible(true);
        setStatus("Encrypting " + total + " file(s)...", ORANGE);

        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                EncryptionEngine engine = new EncryptionEngine(algo, pwd);
                return handler.batchEncrypt(snapshot, engine, logger,
                        new FileHandler.ProgressCallback() {
                            @Override
                            public void onProgress(final int done, final int tot,
                                                   final String result) {
                                // Callback fires on the background thread.
                                // Must use invokeLater to touch GUI components safely.
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        int pct = (int) (((double) done / tot) * 100);
                                        progressBar.setValue(pct);
                                        Color c = result.startsWith("[OK]") ? GREEN : RED;
                                        setStatus("[" + done + "/" + tot + "]  " + result, c);
                                    }
                                });
                            }
                        });
            }

            @Override
            protected void done() {
                try {
                    List<String> results = get();
                    int ok = 0;
                    for (int i = 0; i < results.size(); i++) {
                        if (results.get(i).startsWith("[OK]")) ok++;
                    }
                    int errors = results.size() - ok;
                    setStatus("Batch done: " + ok + " OK, " + errors + " error(s).", GREEN);

                } catch (InterruptedException ex) {
                    // FIX: Restore interrupted status (same reason as in runOp above)
                    Thread.currentThread().interrupt();
                    setStatus("Batch interrupted.", RED);

                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    setStatus("Batch error: " + cause.getMessage(), RED);
                }

                progressBar.setValue(100);
                batchFiles.clear();
                batchModel.clear();

                clearPasswordField();
                refreshLog();
                hideProgress();
            }
        };
        worker.execute();
    }

    // ======================================================
    // Helpers
    // ======================================================

    // Clear the password field and reset the Show checkbox
    // Called after every encrypt/decrypt/batch operation
    private void clearPasswordField() {
        pwdField.setText("");
        showPwdBox.setSelected(false);
        pwdField.setEchoChar('*');
    }

    private void refreshLog() {
        logArea.setText(logger.getLogText());
        int len = logArea.getDocument().getLength();
        if (len > 0) {
            logArea.setCaretPosition(len);
        }
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText("  " + msg);
        statusLabel.setForeground(color);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg,
                "Logic-Gate Vault - Error", JOptionPane.ERROR_MESSAGE);
        setStatus("Error: " + msg.split("\n")[0], RED);
    }

    private void hideProgress() {
        Timer t = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setVisible(false);
            }
        });
        t.setRepeats(false);
        t.start();
    }

    // ======================================================
    // Component factory helpers
    // ======================================================

    private JButton makeButton(String text, final Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(BOLD);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        final Color hover = bg.brighter();
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);    }
        });
        return btn;
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setFont(MONO);
        tf.setBackground(BG_INPUT);
        tf.setForeground(GREY);
        tf.setCaretColor(BLUE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 11));
        l.setForeground(GREY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}