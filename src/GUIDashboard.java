import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

// GUIDashboard.java
// Main Swing window for Logic-Gate Vault v2.0
// Arshpreet Singh | S25CSEU0980

public class GUIDashboard {

    private JFrame frame;
    private JTextField fileField;
    private JComboBox<String> algoBox;
    private JPasswordField pwdField;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private DefaultListModel<String> batchModel;

    private final ActivityLogger logger = new ActivityLogger();
    private final FileHandler handler = new FileHandler();
    private final List<String> batchFiles = new ArrayList<String>();

    // colours
    private static final Color BG       = new Color(7, 8, 15);
    private static final Color BG_CARD  = new Color(12, 18, 38);
    private static final Color BG_INPUT = new Color(15, 20, 40);
    private static final Color BLUE     = new Color(26, 140, 255);
    private static final Color BLUE2    = new Color(0, 85, 187);
    private static final Color WHITE    = new Color(238, 244, 255);
    private static final Color GREY     = new Color(110, 136, 176);
    private static final Color GREEN    = new Color(0, 212, 138);
    private static final Color ORANGE   = new Color(255, 176, 32);
    private static final Color RED      = new Color(255, 85, 51);
    private static final Color BORDER   = new Color(28, 40, 72);

    // fonts
    private static final Font MONO  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font TITLE = new Font("Consolas", Font.BOLD, 16);
    private static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    public void launchGUI() {
        frame = new JFrame("Logic-Gate Vault  v2.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setMinimumSize(new Dimension(820, 580));
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

    // ---- header bar ----

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BLUE));
        bar.setPreferredSize(new Dimension(0, 44));

        JLabel title = new JLabel("  THE LOGIC-GATE VAULT");
        title.setFont(TITLE);
        title.setForeground(BLUE);

        JLabel sub = new JLabel("File Encryption System  |  AES-256 + XOR  |  S25CSEU0980  ");
        sub.setFont(SMALL);
        sub.setForeground(GREY);

        bar.add(title, BorderLayout.WEST);
        bar.add(sub, BorderLayout.EAST);
        return bar;
    }

    // ---- split pane ----

    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildLogPanel());
        split.setDividerLocation(430);
        split.setDividerSize(4);
        split.setBackground(BG);
        split.setBorder(null);
        return split;
    }

    // ---- left controls panel ----

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 8));

        p.add(label("1.  Select File"));
        p.add(Box.createVerticalStrut(6));
        p.add(buildFileRow());
        p.add(Box.createVerticalStrut(14));

        p.add(label("2.  Algorithm"));
        p.add(Box.createVerticalStrut(6));
        p.add(buildAlgoRow());
        p.add(Box.createVerticalStrut(14));

        p.add(label("3.  Password / Key"));
        p.add(Box.createVerticalStrut(6));
        p.add(buildPasswordRow());
        p.add(Box.createVerticalStrut(18));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(sep);
        p.add(Box.createVerticalStrut(14));

        p.add(buildActionButtons());
        p.add(Box.createVerticalStrut(14));

        p.add(label("4.  Batch Mode"));
        p.add(Box.createVerticalStrut(6));
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

    private JPanel buildFileRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        fileField = styledField("No file selected");
        fileField.setEditable(false);

        JButton btn = makeButton("Browse...", BLUE2);
        btn.setPreferredSize(new Dimension(90, 30));
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pickFile();
            }
        });

        row.add(fileField, BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        return row;
    }

    private JPanel buildAlgoRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        algoBox = new JComboBox<String>(new String[]{"XOR", "AES-256"});
        algoBox.setFont(MONO);
        algoBox.setBackground(BG_INPUT);
        algoBox.setForeground(WHITE);
        algoBox.setPreferredSize(new Dimension(150, 30));
        row.add(algoBox);
        return row;
    }

    private JPanel buildPasswordRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        pwdField = new JPasswordField();
        pwdField.setFont(MONO);
        pwdField.setBackground(BG_INPUT);
        pwdField.setForeground(BLUE);
        pwdField.setCaretColor(BLUE);
        pwdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 8, 4, 8)));

        final JCheckBox show = new JCheckBox("Show");
        show.setFont(SMALL);
        show.setForeground(GREY);
        show.setBackground(BG);
        show.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (show.isSelected()) {
                    pwdField.setEchoChar((char) 0);
                } else {
                    pwdField.setEchoChar('*');
                }
            }
        });

        row.add(pwdField, BorderLayout.CENTER);
        row.add(show, BorderLayout.EAST);
        return row;
    }

    private JPanel buildActionButtons() {
        JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton enc = makeButton("ENCRYPT", GREEN);
        JButton dec = makeButton("DECRYPT", BLUE);

        enc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runOp(true);
            }
        });
        dec.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runOp(false);
            }
        });

        row.add(enc);
        row.add(dec);
        return row;
    }

    private JPanel buildBatchSection() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        batchModel = new DefaultListModel<String>();
        JList<String> list = new JList<String>(batchModel);
        list.setFont(SMALL);
        list.setBackground(BG_CARD);
        list.setForeground(WHITE);
        list.setSelectionBackground(BLUE2);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(300, 80));
        scroll.setMinimumSize(new Dimension(100, 60));

        JPanel btns = new JPanel(new GridLayout(1, 3, 6, 0));
        btns.setBackground(BG);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton add   = makeButton("Add Files",     BLUE2);
        JButton batch = makeButton("Batch Encrypt", GREEN);
        JButton clr   = makeButton("Clear",         RED);

        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pickBatchFiles(); }
        });
        batch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { runBatch(); }
        });
        clr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                batchFiles.clear();
                batchModel.clear();
            }
        });

        btns.add(add);
        btns.add(batch);
        btns.add(clr);

        p.add(scroll, BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ---- right log panel ----

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, BORDER));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(8, 12, 6, 8));

        JLabel title = new JLabel("activity.log  |  Timestamped Audit Trail");
        title.setFont(new Font("Consolas", Font.BOLD, 12));
        title.setForeground(BLUE);

        JButton clrBtn = makeButton("Clear Log", RED);
        clrBtn.setFont(SMALL);
        clrBtn.setPreferredSize(new Dimension(80, 24));
        clrBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.clearLog();
                refreshLog();
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(clrBtn, BorderLayout.EAST);

        logArea = new JTextArea();
        logArea.setFont(MONO);
        logArea.setBackground(new Color(5, 6, 16));
        logArea.setForeground(GREEN);
        logArea.setCaretColor(GREEN);
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(8, 10, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(5, 6, 16));

        p.add(header, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ---- footer status bar ----

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        bar.setPreferredSize(new Dimension(0, 28));

        statusLabel = new JLabel("  Initialising...");
        statusLabel.setFont(MONO);
        statusLabel.setForeground(GREY);

        JLabel credit = new JLabel("V8 Logic Systems  |  github.com/Arsh-2k/The-Logic-Gate-Vault  ");
        credit.setFont(SMALL);
        credit.setForeground(new Color(55, 75, 115));

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(credit, BorderLayout.EAST);
        return bar;
    }

    // ---- file picker ----

    private void pickFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select File to Encrypt or Decrypt");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Supported files", "txt", "docx", "xlsx", "pdf", "enc", "png", "jpg", "zip"));
        fc.setAcceptAllFileFilterUsed(true);

        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(fc.getSelectedFile().getAbsolutePath());
            setStatus("File selected: " + fc.getSelectedFile().getName(), WHITE);
        }
    }

    private void pickBatchFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Select Files for Batch Encryption");

        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File[] selected = fc.getSelectedFiles();
            for (File f : selected) {
                String abs = f.getAbsolutePath();
                if (!batchFiles.contains(abs)) {
                    batchFiles.add(abs);
                    batchModel.addElement(f.getName());
                }
            }
            setStatus(batchFiles.size() + " file(s) queued.", ORANGE);
        }
    }

    // ---- single file encrypt/decrypt ----

    private void runOp(final boolean encrypt) {
        final String path = fileField.getText().trim();
        if (path.isEmpty() || "No file selected".equals(path)) {
            showError("No file selected. Click Browse first.");
            return;
        }

        final String pwd = new String(pwdField.getPassword()).trim();
        if (pwd.isEmpty()) {
            showError("Enter a password before continuing.");
            return;
        }

        Object selected = algoBox.getSelectedItem();
        final String algo = "AES-256".equals(selected) ? "AES" : "XOR";

        setStatus("Processing...", ORANGE);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);

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

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                try {
                    setStatus(get(), GREEN);
                } catch (InterruptedException ex) {
                    setStatus("Interrupted.", RED);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null
                            ? cause.getMessage() : "Unknown error";
                    setStatus("Error: " + msg, RED);
                    logger.logError(encrypt ? "ENCRYPT" : "DECRYPT", msg);
                }
                refreshLog();
                hideProgress();
            }
        };
        worker.execute();
    }

    // ---- batch encrypt ----

    private void runBatch() {
        if (batchFiles.isEmpty()) {
            showError("No files queued. Click Add Files first.");
            return;
        }

        final String pwd = new String(pwdField.getPassword()).trim();
        if (pwd.isEmpty()) {
            showError("Enter a password first.");
            return;
        }

        Object selected = algoBox.getSelectedItem();
        final String algo = "AES-256".equals(selected) ? "AES" : "XOR";
        final int total = batchFiles.size();
        final List<String> snapshot = new ArrayList<String>(batchFiles);

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
                            public void onProgress(final int done, final int tot,
                                                   final String result) {
                                SwingUtilities.invokeLater(new Runnable() {
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
                    for (String r : results) {
                        if (r.startsWith("[OK]")) ok++;
                    }
                    int err = results.size() - ok;
                    setStatus("Done: " + ok + " OK, " + err + " error(s).", GREEN);
                } catch (InterruptedException ex) {
                    setStatus("Batch interrupted.", RED);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    setStatus("Batch error: " + cause.getMessage(), RED);
                }
                progressBar.setValue(100);
                batchFiles.clear();
                batchModel.clear();
                refreshLog();
                hideProgress();
            }
        };
        worker.execute();
    }

    // ---- helpers ----

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
                "Logic-Gate Vault", JOptionPane.ERROR_MESSAGE);
        setStatus("Error: " + msg, RED);
    }

    private void hideProgress() {
        Timer t = new Timer(3000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                progressBar.setVisible(false);
            }
        });
        t.setRepeats(false);
        t.start();
    }

    // ---- component helpers ----

    private JButton makeButton(String text, final Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(BOLD);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        final Color hover = bg.brighter();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
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