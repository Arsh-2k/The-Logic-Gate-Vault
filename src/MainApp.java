// File: MainApp.java
// Part of The Logic-Gate Vault
// Team V8 Logic Systems | Arshpreet Singh | S25CSEU0980
// Course: 2025CSET152 | Bennett University

// How to compile and run:
//   javac -encoding UTF-8 -d out src\*.java
//   java -cp out MainApp

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Module M3 — Swing GUI and main entry point.
//
// LAUNCH FLOW:
//   main() -> showLaunchScreen() -> [ADMIN] or [USER] button
//          -> showRoleLoginDialog(role) -> buildMainWindow()
//
// CHANGES IN v5 (this version):
//   1. REQ-1  : How-To text updated with explicit file-type support and
//               ~256 MB size limit so users know what to expect.
//   2. REQ-2  : "Show Password" checkbox added to the LOGIN DIALOG.
//               Every password field in the application now has one.
//   3. REQ-3a : Failed logins (wrong credentials, wrong role) are now logged
//               via ActivityLogger.logEvent() — previously silent.
//   3. REQ-3b : Successful logins also logged for a complete audit trail.
//   3. REQ-3c : buildRightPanel() is now role-aware:
//               ADMIN sees "MONITORING CONSOLE" header + Refresh button.
//   4. REQ-5  : handleCloudSync() shows a WIP transparency notice BEFORE
//               the progress dialog — user clicks OK to proceed to demo.
public class MainApp {

    // -----------------------------------------------------------------------
    // Application state
    // -----------------------------------------------------------------------
    private static String           currentUser  = null;
    private static AuthManager.Role currentRole  = null;
    private static FileOrchestrator orchestrator = null;

    // -----------------------------------------------------------------------
    // Session timeout — 15-minute inactivity via ScheduledExecutorService
    // -----------------------------------------------------------------------
    private ScheduledExecutorService sessionTimerService;
    private ScheduledFuture<?>       sessionTimeoutFuture;
    private static final int         SESSION_TIMEOUT_MINUTES = 15;

    // -----------------------------------------------------------------------
    // GUI component references
    // -----------------------------------------------------------------------
    private JFrame              mainFrame;
    private JTextField          filePathField;
    private JPasswordField      passwordField;
    private JComboBox<String>   algorithmDropdown;
    private JProgressBar        progressBar;
    private JTextArea           logTextArea;
    private DefaultListModel<String> batchListModel;
    private ArrayList<File>     batchFiles;
    private JLabel              statusLabel;
    private JLabel              strengthLabel;

    // -----------------------------------------------------------------------
    // Colour palette — dark hacker aesthetic
    // -----------------------------------------------------------------------
    private static final Color BG_DARK    = new Color(10,  12,  20);
    private static final Color BG_PANEL   = new Color(18,  20,  30);
    private static final Color BG_CARD    = new Color(24,  26,  40);
    private static final Color BG_FIELD   = new Color(32,  34,  50);
    private static final Color CLR_GREEN  = new Color(0,   230, 118);
    private static final Color CLR_CYAN   = new Color(0,   210, 255);
    private static final Color CLR_RED    = new Color(255, 75,  75);
    private static final Color CLR_ORANGE = new Color(255, 165, 0);
    private static final Color CLR_YELLOW = new Color(255, 220, 50);
    private static final Color CLR_PURPLE = new Color(180, 100, 255);
    private static final Color CLR_DRIVE  = new Color(66,  133, 244);
    private static final Color CLR_TEXT   = new Color(210, 220, 230);
    private static final Color CLR_GRAY   = new Color(120, 130, 150);
    private static final Color CLR_BORDER = new Color(45,  50,  70);

    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_BOLD  = new Font("Consolas", Font.BOLD,  12);
    private static final Font FONT_SMALL = new Font("Consolas", Font.PLAIN, 10);

    // -----------------------------------------------------------------------
    // ASCII ART — three blocks, each rendered as individual centred JLabels.
    //
    // Block 1 : "THE"        (CLR_CYAN,  12 pt)
    // Block 2 : "LOGIC-GATE" (CLR_GREEN, 11 pt)
    // Block 3 : "VAULT"      (CLR_GREEN, 11 pt)
    //
    // Using one JLabel per line and wrapping each in a FlowLayout.CENTER panel
    // is the only reliable way to centre block-drawing characters in BoxLayout.
    // JTextArea ignores setAlignmentX(CENTER_ALIGNMENT) entirely.
    // -----------------------------------------------------------------------
    private static final String[] THE_LINES = {
        "  \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2557  \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557  ",
        "  \u255a\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255d\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d  ",
        "     \u2588\u2588\u2551   \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2557    ",
        "     \u2588\u2588\u2551   \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u255d    ",
        "     \u2588\u2588\u2551   \u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557  ",
        "     \u255a\u2550\u255d   \u255a\u2550\u255d  \u255a\u2550\u255d\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d  "
    };

    private static final String[] LOGIC_GATE_LINES = {
        " \u2588\u2588\u2557      \u2588\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557        \u2588\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557 ",
        " \u2588\u2588\u2551     \u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d \u2588\u2588\u2551 \u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d       \u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u255a\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255d\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255d ",
        " \u2588\u2588\u2551     \u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2588\u2557\u2588\u2588\u2551 \u2588\u2588\u2551     \u2500\u2500\u2500\u2500\u2500  \u2588\u2588\u2551  \u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2588\u2588\u2588\u2557   ",
        " \u2588\u2588\u2551     \u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551 \u2588\u2588\u2551            \u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2554\u2550\u2550\u255d   ",
        " \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2588\u2557       \u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u2588\u2588\u2551  \u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557 ",
        " \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u2550\u2550\u2550\u2550\u255d  \u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u2550\u255d        \u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u255d  \u255a\u2550\u255d   \u255a\u2550\u255d   \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d "
    };

    private static final String[] VAULT_LINES = {
        "   \u2588\u2588\u2557   \u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557   ",
        "   \u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551  \u255a\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255d   ",
        "   \u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551     \u2588\u2588\u2551       ",
        "   \u255a\u2588\u2588\u2557 \u2588\u2588\u2554\u255d\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2551     \u2588\u2588\u2551       ",
        "    \u255a\u2588\u2588\u2588\u2588\u2554\u255d \u2588\u2588\u2551  \u2588\u2588\u2551\u255a\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2551       ",
        "     \u255a\u2550\u2550\u2550\u255d  \u255a\u2550\u255d  \u255a\u2550\u255d \u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u255d\u255a\u2550\u255d       "
    };

    // -----------------------------------------------------------------------
    // ENTRY POINT
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { /* not critical */ }

        SwingUtilities.invokeLater(() -> new MainApp().showLaunchScreen());
    }

    // -----------------------------------------------------------------------
    // LAUNCH SCREEN
    // Outer panel: GridBagLayout (anchor=CENTER, fill=NONE) gives contentPanel
    // its natural width and centres it on screen.
    // contentPanel: BoxLayout Y_AXIS stacks all elements vertically.
    // Each ASCII line: JLabel inside a FlowLayout.CENTER wrapper — the wrapper
    // is stretched to full width by setMaximumSize(MAX_INT, h) so FlowLayout
    // places the JLabel exactly at the horizontal mid-point.
    // -----------------------------------------------------------------------
    private void showLaunchScreen() {
        JFrame launch = new JFrame();
        launch.setUndecorated(true);
        launch.setExtendedState(JFrame.MAXIMIZED_BOTH);
        launch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        launch.getRootPane().registerKeyboardAction(
            e -> System.exit(0),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(BG_DARK);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        addAsciiBlock(contentPanel, THE_LINES,        CLR_CYAN,  12);
        contentPanel.add(vGap(8));
        addAsciiBlock(contentPanel, LOGIC_GATE_LINES, CLR_GREEN, 11);
        contentPanel.add(vGap(6));
        addAsciiBlock(contentPanel, VAULT_LINES,      CLR_GREEN, 11);
        contentPanel.add(vGap(22));

        addCenteredLabel(contentPanel,
            "AES-256  \u00b7  XOR  \u00b7  RBAC  \u00b7  Key Escrow  \u00b7  Audit Log  \u00b7  Cloud Sync",
            new Font("Consolas", Font.PLAIN, 13), CLR_CYAN);
        contentPanel.add(vGap(6));
        addCenteredLabel(contentPanel,
            "V8 Logic Systems  \u00b7  Arshpreet Singh  \u00b7  S25CSEU0980  \u00b7  2025CSET152  \u00b7  Bennett University",
            FONT_SMALL, CLR_GRAY);
        contentPanel.add(vGap(48));

        addCenteredLabel(contentPanel,
            "SELECT YOUR ROLE TO CONTINUE",
            new Font("Consolas", Font.BOLD, 15), new Color(155, 165, 185));
        contentPanel.add(vGap(28));

        JButton adminBtn = makeLaunchButton(
            "  ADMIN  ", CLR_PURPLE,
            "Monitor Logs \u00b7 Key Escrow Recovery \u00b7 Clear Log");
        JButton userBtn  = makeLaunchButton(
            "   USER  ", CLR_GREEN,
            "Encrypt \u00b7 Decrypt \u00b7 Batch Encrypt \u00b7 Cloud Sync");

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 0));
        btnRow.setOpaque(false);
        btnRow.add(adminBtn);
        btnRow.add(userBtn);

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btnRow);
        btnWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            adminBtn.getPreferredSize().height + 8));
        contentPanel.add(btnWrap);
        contentPanel.add(vGap(20));

        addCenteredLabel(contentPanel, "[ ESC ] to exit",
            FONT_SMALL, new Color(52, 58, 78));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.anchor  = GridBagConstraints.CENTER;
        gbc.fill    = GridBagConstraints.NONE;
        outerPanel.add(contentPanel, gbc);

        JPanel strip = new JPanel(new BorderLayout());
        strip.setBackground(new Color(7, 9, 15));
        strip.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        JLabel sLeft = new JLabel(
            "AES-256/CBC/PKCS5  \u00b7  SHA-256 MAC  \u00b7  PBKDF2WithHmacSHA256  \u00b7  15-min session timeout");
        sLeft.setFont(FONT_SMALL);
        sLeft.setForeground(new Color(42, 48, 65));
        JLabel sRight = new JLabel("github.com/Arsh-2k/The-Logic-Gate-Vault", JLabel.RIGHT);
        sRight.setFont(FONT_SMALL);
        sRight.setForeground(new Color(42, 48, 65));
        strip.add(sLeft, BorderLayout.WEST);
        strip.add(sRight, BorderLayout.EAST);

        launch.setLayout(new BorderLayout());
        launch.add(outerPanel, BorderLayout.CENTER);
        launch.add(strip,      BorderLayout.SOUTH);

        adminBtn.addActionListener(e -> {
            launch.dispose();
            showRoleLoginDialog(AuthManager.Role.ADMIN);
        });
        userBtn.addActionListener(e -> {
            launch.dispose();
            showRoleLoginDialog(AuthManager.Role.USER);
        });

        launch.setVisible(true);
    }

    // Each ASCII art line as a centred JLabel inside a FlowLayout.CENTER wrapper.
    // The wrapper's MAX width is set to Integer.MAX_VALUE so BoxLayout stretches it
    // to the full content-panel width; FlowLayout then centres the JLabel within it.
    private void addAsciiBlock(JPanel container, String[] lines, Color color, int fontSize) {
        Font f = new Font("Consolas", Font.BOLD, fontSize);
        for (String line : lines) {
            JLabel lbl = new JLabel(line, JLabel.CENTER);
            lbl.setFont(f);
            lbl.setForeground(color);

            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            wrap.setOpaque(false);
            wrap.add(lbl);
            wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                lbl.getPreferredSize().height + 4));
            container.add(wrap);
        }
    }

    // Single centred label — same FlowLayout.CENTER wrapper technique.
    private void addCenteredLabel(JPanel container, String text, Font font, Color color) {
        JLabel lbl = new JLabel(text, JLabel.CENTER);
        lbl.setFont(font);
        lbl.setForeground(color);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);
        wrap.add(lbl);
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            lbl.getPreferredSize().height + 4));
        container.add(wrap);
    }

    // -----------------------------------------------------------------------
    // ROLE-SPECIFIC LOGIN DIALOG
    //
    // FIX REQ-2 (v5): "Show Password" checkbox added to the password row.
    //   Previously the login dialog had no Show toggle; the main encrypt/decrypt
    //   fields did.  Every password field in the application now has one.
    //
    // FIX REQ-3 (v5): All login outcomes are now logged via logEvent():
    //   - Successful login  → LOGIN_SUCCESS with role
    //   - Wrong credentials → LOGIN_FAILED
    //   - Wrong role        → LOGIN_ROLE_MISMATCH
    //   This completes the audit trail that the Admin monitors.
    // -----------------------------------------------------------------------
    private void showRoleLoginDialog(AuthManager.Role requestedRole) {
        AuthManager authManager = new AuthManager();

        boolean isAdmin   = requestedRole == AuthManager.Role.ADMIN;
        String  roleLabel = isAdmin ? "ADMIN"   : "USER";
        Color   roleColor = isAdmin ? CLR_PURPLE : CLR_GREEN;
        String  hintText  = isAdmin
            ? "Credentials:  admin  /  Admin@123"
            : "Credentials:  user  /  User@123";

        JDialog dialog = new JDialog();
        dialog.setTitle("Logic-Gate Vault \u2014 " + roleLabel + " Login");
        dialog.setSize(430, 360);   // slightly taller to accommodate the Show checkbox row
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(BG_DARK);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(28, 40, 24, 40));

        // Role badge
        JLabel roleBadge = new JLabel("[ " + roleLabel + " ]", JLabel.CENTER);
        roleBadge.setFont(new Font("Consolas", Font.BOLD, 22));
        roleBadge.setForeground(roleColor);
        roleBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(roleBadge);
        root.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel sub = new JLabel("THE LOGIC-GATE VAULT", JLabel.CENTER);
        sub.setFont(new Font("Consolas", Font.PLAIN, 11));
        sub.setForeground(CLR_GRAY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(sub);
        root.add(Box.createRigidArea(new Dimension(0, 24)));

        // Username row
        JPanel uRow = new JPanel(new BorderLayout(10, 0));
        uRow.setBackground(BG_DARK);
        uRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        uRow.add(makeFormLabel("Username"), BorderLayout.WEST);
        JTextField usernameField = makeDarkTextField();
        uRow.add(usernameField, BorderLayout.CENTER);
        root.add(uRow);
        root.add(Box.createRigidArea(new Dimension(0, 10)));

        // Password row — FIX REQ-2: Show checkbox added to EAST
        JPanel pRow = new JPanel(new BorderLayout(10, 0));
        pRow.setBackground(BG_DARK);
        pRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        pRow.add(makeFormLabel("Password"), BorderLayout.WEST);
        JPasswordField passField = new JPasswordField();
        stylePasswordField(passField);
        pRow.add(passField, BorderLayout.CENTER);

        JCheckBox loginShowBox = new JCheckBox("Show");
        loginShowBox.setBackground(BG_DARK);
        loginShowBox.setForeground(CLR_GRAY);
        loginShowBox.setFont(FONT_SMALL);
        loginShowBox.setFocusPainted(false);
        // Toggle echo char: (char)0 shows plaintext; '\u2022' shows bullet dots
        loginShowBox.addActionListener(e ->
            passField.setEchoChar(loginShowBox.isSelected() ? (char) 0 : '\u2022'));
        pRow.add(loginShowBox, BorderLayout.EAST);
        root.add(pRow);
        root.add(Box.createRigidArea(new Dimension(0, 8)));

        // Error label (invisible until needed)
        JLabel errorLbl = new JLabel(" ", JLabel.CENTER);
        errorLbl.setFont(FONT_SMALL);
        errorLbl.setForeground(CLR_RED);
        errorLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(errorLbl);
        root.add(Box.createRigidArea(new Dimension(0, 10)));

        // Login button
        JButton loginBtn = makeButton("  LOGIN AS " + roleLabel + "  ", roleColor);
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        root.add(loginBtn);
        root.add(Box.createRigidArea(new Dimension(0, 8)));

        // Hint label (low-visibility credentials reminder)
        JLabel hintLbl = new JLabel(hintText, JLabel.CENTER);
        hintLbl.setFont(new Font("Consolas", Font.PLAIN, 10));
        hintLbl.setForeground(new Color(58, 63, 85));
        hintLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(hintLbl);
        root.add(Box.createRigidArea(new Dimension(0, 4)));

        // Back link
        JButton backBtn = new JButton("\u2190 Back to role selection");
        backBtn.setBackground(BG_DARK);
        backBtn.setForeground(CLR_GRAY);
        backBtn.setFont(FONT_SMALL);
        backBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        backBtn.setFocusPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(backBtn);

        dialog.add(root);

        // ── Login logic — runs on every Submit attempt ──
        Runnable doLogin = () -> {
            String username  = usernameField.getText().trim(); // edge case: trim whitespace
            char[] loginPass = passField.getPassword();

            // ── Blank-field guard ──
            if (username.isEmpty() || loginPass.length == 0) {
                errorLbl.setText("Please enter both username and password.");
                Arrays.fill(loginPass, '\0'); // memory security — zero immediately
                return;
            }

            AuthManager.Role authenticated = authManager.authenticate(username, loginPass);
            Arrays.fill(loginPass, '\0'); // memory security — zero immediately after auth

            // ── FIX REQ-3: log failed logins (previously silent) ──
            if (authenticated == null) {
                // Wrong username or wrong password — credentials not found
                ActivityLogger.getInstance().logEvent(
                    username, "LOGIN_FAILED",
                    "Invalid credentials on " + roleLabel + " screen");
                errorLbl.setText("Invalid credentials. Please try again.");
                passField.setText("");
                return;
            }
            // ── FIX REQ-3: log role-mismatch attempts ──
            if (authenticated != requestedRole) {
                // Valid credentials but wrong role button (e.g. admin creds on USER screen)
                ActivityLogger.getInstance().logEvent(
                    username, "LOGIN_ROLE_MISMATCH",
                    "Credentials match " + authenticated.name()
                    + " but used on " + roleLabel + " screen");
                errorLbl.setText("Those credentials do not match the " + roleLabel + " role.");
                passField.setText("");
                return;
            }

            // ── FIX REQ-3: log successful logins ──
            ActivityLogger.getInstance().logEvent(
                username, "LOGIN_SUCCESS",
                "Role: " + authenticated.name());

            currentUser  = username;
            currentRole  = authenticated;
            orchestrator = new FileOrchestrator(currentUser, currentRole);
            dialog.dispose();
            buildMainWindow();
            startSessionTimer();
        };

        loginBtn.addActionListener(e -> doLogin.run());
        // Enter in username → focus jumps to password (keyboard nav edge case)
        usernameField.addActionListener(e -> passField.requestFocusInWindow());
        // Enter in password field → submit
        passField.addActionListener(e -> doLogin.run());

        // Back button just disposes — the null-check after setVisible() below handles
        // returning to the launch screen.  Never call showLaunchScreen() here directly
        // because that would create a second launch window while this thread continues.
        backBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true); // BLOCKS until dialog is disposed

        // Called when dialog closes without a successful login (X button or Back button).
        if (currentUser == null) {
            showLaunchScreen();
        }
    }

    // -----------------------------------------------------------------------
    // SESSION TIMEOUT
    // -----------------------------------------------------------------------
    private void startSessionTimer() {
        sessionTimerService = Executors.newSingleThreadScheduledExecutor();
        resetSessionTimer();
    }

    private void resetSessionTimer() {
        if (sessionTimerService == null || sessionTimerService.isShutdown()) return;
        if (sessionTimeoutFuture != null && !sessionTimeoutFuture.isDone()) {
            sessionTimeoutFuture.cancel(false);
        }
        sessionTimeoutFuture = sessionTimerService.schedule(
            this::handleSessionExpired, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    private void handleSessionExpired() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(mainFrame,
                "Session expired after " + SESSION_TIMEOUT_MINUTES
                + " minutes of inactivity.\nReturning to role selection.",
                "Session Expired", JOptionPane.WARNING_MESSAGE);
            ActivityLogger.getInstance().logEvent(
                currentUser, "SESSION_TIMEOUT",
                "Auto-logout after " + SESSION_TIMEOUT_MINUTES + " min");
            stopSessionTimer();
            if (mainFrame != null) mainFrame.dispose();
            currentUser  = null;
            currentRole  = null;
            orchestrator = null;
            showLaunchScreen();
        });
    }

    private void stopSessionTimer() {
        if (sessionTimeoutFuture != null) sessionTimeoutFuture.cancel(false);
        if (sessionTimerService  != null) sessionTimerService.shutdownNow();
    }

    // -----------------------------------------------------------------------
    // MAIN APPLICATION WINDOW
    // -----------------------------------------------------------------------
    private void buildMainWindow() {
        mainFrame = new JFrame(
            "The Logic-Gate Vault v2.0  \u00b7  S25CSEU0980  \u00b7  "
            + (currentRole == AuthManager.Role.ADMIN ? "ADMIN" : "USER")
            + ": " + currentUser);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1150, 700);
        mainFrame.setMinimumSize(new Dimension(920, 620));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.getContentPane().setBackground(BG_DARK);
        mainFrame.setLayout(new BorderLayout());

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { stopSessionTimer(); }
        });

        batchFiles     = new ArrayList<>();
        batchListModel = new DefaultListModel<>();

        mainFrame.add(buildHeaderBar(),  BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(470);
        split.setDividerSize(2);
        split.setBorder(null);
        mainFrame.add(split, BorderLayout.CENTER);
        mainFrame.add(buildStatusBar(), BorderLayout.SOUTH);

        refreshLogDisplay();
        mainFrame.setVisible(true);
    }

    // ---- Header bar ----------------------------------------------------------
    private JPanel buildHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(8, 10, 18));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JLabel title = new JLabel("THE LOGIC-GATE VAULT");
        title.setFont(new Font("Consolas", Font.BOLD, 17));
        title.setForeground(CLR_GREEN);

        String roleTag = currentRole == AuthManager.Role.ADMIN ? "[ ADMIN ]" : "[ USER ]";
        Color  roleCol = currentRole == AuthManager.Role.ADMIN ? CLR_PURPLE : CLR_GREEN;
        JLabel roleLabel = new JLabel(roleTag + "  " + currentUser);
        roleLabel.setFont(new Font("Consolas", Font.BOLD, 11));
        roleLabel.setForeground(roleCol);

        JLabel infoLabel = new JLabel(
            "AES-256 + XOR  \u00b7  S25CSEU0980  \u00b7  Session: 15-min timeout", JLabel.RIGHT);
        infoLabel.setFont(FONT_SMALL);
        infoLabel.setForeground(CLR_GRAY);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.setBackground(new Color(8, 10, 18));
        rightPanel.add(roleLabel);
        rightPanel.add(infoLabel);

        header.add(title,      BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ---- Left control panel --------------------------------------------------
    // FIX REQ-1 (v5): How-To text now explicitly states:
    //   • ALL file types are supported (txt, pdf, mp4, zip, docx, …)
    //   • Files up to ~256 MB supported; OOM is caught cleanly above that
    //   • What each button actually does (cause and effect)
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        panel.add(makeSectionLabel("How To Use  |  System Limits"));
        JTextArea howTo = new JTextArea(
              "SUPPORTS: ALL file types \u2014 txt, pdf, docx, xlsx, jpg, png,\n"
            + "          mp4, zip, exe, bin, and any other format.\n"
            + "LIMIT:    Files up to ~256 MB. Larger files may cause an\n"
            + "          out-of-memory error (caught and shown clearly).\n\n"
            + "ENCRYPT : Browse any file \u2192 pick algorithm \u2192 enter password\n"
            + "          \u2192 click ENCRYPT. Original file is DELETED and replaced\n"
            + "          with a .enc file. Use the SAME password to decrypt.\n\n"
            + "DECRYPT : Select the .enc file \u2192 pick the SAME algorithm\n"
            + "          \u2192 enter the SAME password \u2192 click DECRYPT.\n"
            + "          .enc is DELETED; original file is restored.\n\n"
            + "BATCH   : Add Files \u2192 set password \u2192 Batch Encrypt.\n"
            + "          Runs in background \u2014 GUI stays responsive.\n\n"
            + "SYNC    : Select a .enc file \u2192 Sync to Drive (simulated demo).\n\n"
            + "PASSWORD: STRONG = 8+ chars + special char (@#$!) + digit (0-9)");
        howTo.setEditable(false);
        howTo.setBackground(new Color(14, 16, 26));
        howTo.setForeground(CLR_GRAY);
        howTo.setFont(FONT_SMALL);
        howTo.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        howTo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(howTo);
        panel.add(vGap(10));

        // 1. File selection
        panel.add(makeSectionLabel("1.  Select File"));
        JPanel fileRow = new JPanel(new BorderLayout(5, 0));
        fileRow.setBackground(BG_PANEL);
        fileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        fileRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePathField = makeDarkTextField();
        filePathField.setText("No file selected...");
        filePathField.setEditable(false);
        JButton browseBtn = makeButton("Browse...", CLR_CYAN);
        browseBtn.setPreferredSize(new Dimension(88, 28));
        browseBtn.addActionListener(e -> { resetSessionTimer(); browseFile(); });
        fileRow.add(filePathField, BorderLayout.CENTER);
        fileRow.add(browseBtn,     BorderLayout.EAST);
        panel.add(fileRow);
        panel.add(vGap(10));

        // 2. Algorithm
        panel.add(makeSectionLabel("2.  Algorithm"));
        algorithmDropdown = new JComboBox<>(new String[]{"AES-256", "XOR"});
        algorithmDropdown.setBackground(BG_FIELD);
        algorithmDropdown.setForeground(CLR_TEXT);
        algorithmDropdown.setFont(FONT_MONO);
        algorithmDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        algorithmDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        algorithmDropdown.addActionListener(e -> resetSessionTimer());
        panel.add(algorithmDropdown);
        panel.add(vGap(10));

        // 3. Password + Show checkbox (already present in main window)
        panel.add(makeSectionLabel("3.  Password / Key"));
        JPanel passRow = new JPanel(new BorderLayout(5, 0));
        passRow.setBackground(BG_PANEL);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = new JPasswordField();
        stylePasswordField(passwordField);
        JCheckBox showBox = new JCheckBox("Show");
        showBox.setBackground(BG_PANEL);
        showBox.setForeground(CLR_GRAY);
        showBox.setFont(FONT_SMALL);
        showBox.setFocusPainted(false);
        showBox.addActionListener(e -> {
            resetSessionTimer();
            passwordField.setEchoChar(showBox.isSelected() ? (char) 0 : '\u2022');
        });
        passRow.add(passwordField, BorderLayout.CENTER);
        passRow.add(showBox,       BorderLayout.EAST);
        panel.add(passRow);

        strengthLabel = new JLabel("  Strength: ---");
        strengthLabel.setFont(FONT_SMALL);
        strengthLabel.setForeground(CLR_GRAY);
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(strengthLabel);

        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateStrength(); }
            public void removeUpdate(DocumentEvent e)  { updateStrength(); }
            public void changedUpdate(DocumentEvent e) { updateStrength(); }
        });
        panel.add(vGap(14));

        // Encrypt / Decrypt
        JPanel encDecRow = new JPanel(new GridLayout(1, 2, 8, 0));
        encDecRow.setBackground(BG_PANEL);
        encDecRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        encDecRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton encBtn = makeButton("ENCRYPT", CLR_GREEN);
        JButton decBtn = makeButton("DECRYPT", CLR_CYAN);
        encBtn.addActionListener(e -> { resetSessionTimer(); handleEncrypt(); });
        decBtn.addActionListener(e -> { resetSessionTimer(); handleDecrypt(); });
        encDecRow.add(encBtn);
        encDecRow.add(decBtn);
        panel.add(encDecRow);
        panel.add(vGap(6));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setBackground(BG_FIELD);
        progressBar.setForeground(CLR_GREEN);
        progressBar.setFont(FONT_SMALL);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(progressBar);
        panel.add(vGap(14));

        // 4. Batch mode
        panel.add(makeSectionLabel("4.  Batch Mode  (background thread \u2014 GUI stays live)"));
        JList<String> batchList = new JList<>(batchListModel);
        batchList.setBackground(BG_FIELD);
        batchList.setForeground(CLR_TEXT);
        batchList.setFont(FONT_SMALL);
        JScrollPane batchScroll = new JScrollPane(batchList);
        batchScroll.setBackground(BG_FIELD);
        batchScroll.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        batchScroll.setPreferredSize(new Dimension(0, 70));
        batchScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        batchScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(batchScroll);
        panel.add(vGap(5));

        JPanel batchBtnRow = new JPanel(new GridLayout(1, 4, 5, 0));
        batchBtnRow.setBackground(BG_PANEL);
        batchBtnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        batchBtnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton addBtn      = makeButton("Add Files",     CLR_CYAN);
        JButton batchBtn    = makeButton("Batch Encrypt", CLR_ORANGE);
        JButton batchDecBtn = makeButton("Batch Decrypt", CLR_PURPLE);
        JButton clearBtn    = makeButton("Clear",         CLR_RED);
        addBtn.addActionListener(e ->      { resetSessionTimer(); addFilesToBatch(); });
        batchBtn.addActionListener(e ->    { resetSessionTimer(); handleBatchEncrypt(); });
        batchDecBtn.addActionListener(e -> { resetSessionTimer(); handleBatchDecrypt(); });
        clearBtn.addActionListener(e -> {
            resetSessionTimer(); batchFiles.clear(); batchListModel.clear(); });
        batchBtnRow.add(addBtn);
        batchBtnRow.add(batchBtn);
        batchBtnRow.add(batchDecBtn);
        batchBtnRow.add(clearBtn);
        panel.add(batchBtnRow);
        panel.add(vGap(12));

        // 5. Cloud Sync
        panel.add(makeSectionLabel("5.  Cloud Sync  (Google Drive \u2014 UI demo / Phase 2 WIP)"));
        JButton driveBtn = makeDriveButton();
        driveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        driveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        driveBtn.addActionListener(e -> { resetSessionTimer(); handleCloudSync(); });
        panel.add(driveBtn);

        // 6. Admin Tools (hidden from USER role)
        if (currentRole == AuthManager.Role.ADMIN) {
            panel.add(vGap(12));
            panel.add(makeSectionLabel("6.  Admin Tools"));
            JPanel adminRow = new JPanel(new GridLayout(1, 2, 8, 0));
            adminRow.setBackground(BG_PANEL);
            adminRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            adminRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton escrowBtn   = makeButton("Key Escrow Recovery", CLR_PURPLE);
            JButton clearLogBtn = makeButton("Clear Activity Log",  CLR_RED);
            escrowBtn.addActionListener(e ->   { resetSessionTimer(); handleEscrowRecovery(); });
            clearLogBtn.addActionListener(e -> { resetSessionTimer(); handleClearLog(); });
            adminRow.add(escrowBtn);
            adminRow.add(clearLogBtn);
            panel.add(adminRow);
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ---- Right log panel (P3 audit trail) ------------------------------------
    // FIX REQ-3 (v5): Right panel is now ROLE-AWARE.
    //   ADMIN gets:
    //     • Header text "ADMIN MONITORING CONSOLE" in purple (not cyan)
    //     • A "Refresh" button to pull latest log entries on demand
    //     • Second sub-label: "View all user activity below \u2014 auto-updated after each action"
    //   USER gets:
    //     • Standard "Activity Log" header in cyan, no Refresh button
    // -----------------------------------------------------------------------
    private JPanel buildRightPanel() {
        boolean isAdmin = currentRole == AuthManager.Role.ADMIN;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));

        // ── Header section ──
        JPanel logHeader = new JPanel(new BorderLayout(8, 0));
        logHeader.setBackground(BG_DARK);
        logHeader.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setBackground(BG_DARK);

        if (isAdmin) {
            JLabel monitorTitle = new JLabel("ADMIN MONITORING CONSOLE \u2014 logs/activity.log");
            monitorTitle.setFont(FONT_BOLD);
            monitorTitle.setForeground(CLR_PURPLE);
            titleStack.add(monitorTitle);

            JLabel monitorSub = new JLabel(
                "All user activity below \u2014 auto-refreshed after each operation");
            monitorSub.setFont(FONT_SMALL);
            monitorSub.setForeground(new Color(130, 100, 180));
            titleStack.add(monitorSub);
        } else {
            JLabel logTitle = new JLabel("logs/activity.log  |  P3 Timestamped Audit Trail");
            logTitle.setFont(FONT_BOLD);
            logTitle.setForeground(CLR_CYAN);
            titleStack.add(logTitle);
        }

        logHeader.add(titleStack, BorderLayout.WEST);

        // Admin gets a manual Refresh button for immediate log reload
        if (isAdmin) {
            JButton refreshBtn = makeButton(" \u21ba Refresh ", CLR_PURPLE);
            refreshBtn.setFont(FONT_SMALL);
            refreshBtn.setPreferredSize(new Dimension(90, 24));
            refreshBtn.addActionListener(e -> {
                resetSessionTimer();
                refreshLogDisplay();
                statusLabel.setText("Log refreshed.");
            });
            logHeader.add(refreshBtn, BorderLayout.EAST);
        }

        panel.add(logHeader, BorderLayout.NORTH);

        // ── Log text area ──
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setBackground(new Color(6, 8, 14));
        logTextArea.setForeground(isAdmin ? new Color(210, 180, 255) : CLR_GREEN);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logTextArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        logTextArea.setCaretColor(CLR_GREEN);

        JScrollPane scroll = new JScrollPane(logTextArea);
        scroll.setBorder(BorderFactory.createLineBorder(
            isAdmin ? new Color(100, 60, 160) : CLR_BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---- Status bar ----------------------------------------------------------
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(8, 10, 16));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, CLR_BORDER),
            BorderFactory.createEmptyBorder(3, 12, 3, 12)));
        statusLabel = new JLabel("Ready \u2014 Select a file to begin.");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(CLR_GRAY);
        JLabel right = new JLabel(
            "V8 Logic Systems  \u00b7  github.com/Arsh-2k/The-Logic-Gate-Vault");
        right.setFont(FONT_SMALL);
        right.setForeground(new Color(42, 48, 65));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(right,       BorderLayout.EAST);
        return bar;
    }

    // -----------------------------------------------------------------------
    // GOOGLE DRIVE SYNC
    //
    // FIX REQ-5 (v5): A "Work In Progress" transparency notice is shown BEFORE
    // the progress dialog opens.  This is honest and aligns with the project's
    // academic integrity requirements for a demo feature.
    //
    // The user clicks OK (or presses Enter) to proceed to the demo.
    // Clicking Cancel / closing the dialog aborts the sync entirely.
    // -----------------------------------------------------------------------
    private void handleCloudSync() {
        String filePath = filePathField.getText().trim();

        if (filePath.isEmpty() || filePath.equals("No file selected...")) {
            showWarning("Please select an encrypted .enc file to sync to Drive.");
            return;
        }
        if (!filePath.endsWith(".enc")) {
            showWarning("Only .enc encrypted files can be synced to Google Drive.\n"
                + "Plaintext files will never be uploaded (zero-knowledge guarantee).");
            return;
        }
        if (!new File(filePath).exists()) {
            showError("File not found: " + filePath);
            return;
        }

        // ── FIX REQ-5: WIP transparency popup ──
        // Must appear BEFORE the progress dialog.  User must click OK to continue.
        int wip = JOptionPane.showConfirmDialog(mainFrame,
            "Cloud Sync \u2014 Work In Progress Notice\n\n"
            + "Google Drive integration is currently a PLANNED FEATURE.\n"
            + "It will be fully implemented in a future update using\n"
            + "the Google Drive REST API v3 with OAuth2 authentication.\n\n"
            + "Proceeding to UI Demo \u2014 no real network calls will be made.\n"
            + "Your encrypted file will NOT leave your machine.",
            "Cloud Sync \u2014 Work In Progress",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE);

        if (wip != JOptionPane.OK_OPTION) {
            // User cancelled — do nothing
            return;
        }

        final String shortName   = new File(filePath).getName();
        final String filePathFin = filePath; // effectively final for SwingWorker

        // ── Build the progress dialog ──
        JDialog driveDialog = new JDialog(mainFrame,
            "Google Drive \u2014 Cloud Sync (Demo)", true);
        driveDialog.setSize(520, 255);
        driveDialog.setResizable(false);
        driveDialog.setLocationRelativeTo(mainFrame);
        driveDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        driveDialog.getContentPane().setBackground(BG_DARK);

        JPanel dp = new JPanel();
        dp.setLayout(new BoxLayout(dp, BoxLayout.Y_AXIS));
        dp.setBackground(BG_DARK);
        dp.setBorder(BorderFactory.createEmptyBorder(22, 30, 18, 30));

        JLabel dlTitle = new JLabel("  Google Drive Sync  \u2014  UI Demo", JLabel.LEFT);
        dlTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        dlTitle.setForeground(CLR_DRIVE);
        dlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        dp.add(dlTitle);
        dp.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel fileLabel = new JLabel("  Encrypting only: " + shortName, JLabel.LEFT);
        fileLabel.setFont(FONT_SMALL);
        fileLabel.setForeground(CLR_GRAY);
        fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dp.add(fileLabel);
        dp.add(Box.createRigidArea(new Dimension(0, 18)));

        JLabel stageLabel = new JLabel("  Preparing...", JLabel.LEFT);
        stageLabel.setFont(FONT_MONO);
        stageLabel.setForeground(CLR_TEXT);
        stageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dp.add(stageLabel);
        dp.add(Box.createRigidArea(new Dimension(0, 8)));

        JProgressBar driveBar = new JProgressBar(0, 100);
        driveBar.setStringPainted(true);
        driveBar.setString("0%");
        driveBar.setBackground(BG_FIELD);
        driveBar.setForeground(CLR_DRIVE);
        driveBar.setFont(FONT_SMALL);
        driveBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        driveBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        dp.add(driveBar);
        dp.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel resultLabel = new JLabel(" ", JLabel.LEFT);
        resultLabel.setFont(FONT_SMALL);
        resultLabel.setForeground(CLR_GREEN);
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dp.add(resultLabel);

        driveDialog.add(dp);

        // SwingWorker keeps the UI responsive during the simulated upload
        SwingWorker<String, CloudSyncManager.SimulatedUpload.StageResult> worker =
            new SwingWorker<String, CloudSyncManager.SimulatedUpload.StageResult>() {

                @Override
                protected String doInBackground() throws Exception {
                    CloudSyncManager.SimulatedUpload upload =
                        new CloudSyncManager.SimulatedUpload(shortName);
                    while (upload.hasNextStage()) {
                        CloudSyncManager.SimulatedUpload.StageResult stage = upload.nextStage();
                        publish(stage);
                        int delay = stage.percent > 60 ? 900 : 550;
                        Thread.sleep(delay);
                    }
                    return upload.getAssignedFileId();
                }

                @Override
                protected void process(
                        List<CloudSyncManager.SimulatedUpload.StageResult> stages) {
                    CloudSyncManager.SimulatedUpload.StageResult latest =
                        stages.get(stages.size() - 1);
                    stageLabel.setText("  " + latest.label);
                    driveBar.setValue(latest.percent);
                    driveBar.setString(latest.percent + "%");
                }

                @Override
                protected void done() {
                    try {
                        String fileId = get();

                        // Persist to D4 (.vault) using the SAME fileId shown in dialog
                        new CloudSyncManager().saveVaultEntry(filePathFin, fileId);

                        ActivityLogger.getInstance().log(
                            currentUser, "CLOUD_UPLOAD", shortName, "N/A", 0, 0);

                        driveBar.setForeground(CLR_GREEN);
                        stageLabel.setForeground(CLR_GREEN);
                        stageLabel.setText("  Upload complete!  \u2713");
                        resultLabel.setText(
                            "  Drive FileId: " + fileId
                            + "  \u00b7  Saved to data/.vault");

                        refreshLogDisplay();
                        statusLabel.setText("Cloud sync demo complete: " + shortName);

                        // Auto-close after 2 s, then show summary
                        javax.swing.Timer closeTimer = new javax.swing.Timer(2000, ev -> {
                            driveDialog.dispose();
                            JOptionPane.showMessageDialog(mainFrame,
                                "Demo Upload Successful!\n\n"
                                + "File        : " + shortName + "\n"
                                + "Drive FileId: " + fileId + "\n"
                                + "Metadata    : data/.vault\n\n"
                                + "NOTE: This was a simulated demo.\n"
                                + "Real Drive API integration is planned for Phase 2.",
                                "Google Drive Sync \u2014 Demo Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                        closeTimer.setRepeats(false);
                        closeTimer.start();

                    } catch (Exception ex) {
                        stageLabel.setForeground(CLR_RED);
                        stageLabel.setText("  Upload failed: " + ex.getMessage());
                        driveBar.setForeground(CLR_RED);
                        driveDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    }
                }
            };

        worker.execute();
        driveDialog.setVisible(true); // modal — blocks until auto-closed or error
    }

    // -----------------------------------------------------------------------
    // ACTION HANDLERS
    // -----------------------------------------------------------------------

    private void browseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a File to Encrypt or Decrypt");
        if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            filePathField.setText(chosen.getAbsolutePath());
            statusLabel.setText("File selected: " + chosen.getName());
        }
    }

    private void addFilesToBatch() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select Files for Batch Encryption");
        if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                batchFiles.add(f);
                batchListModel.addElement(f.getName());
            }
        }
    }

    private void handleEncrypt() {
        String filePath = filePathField.getText().trim();
        char[] password = passwordField.getPassword();
        String algo     = (String) algorithmDropdown.getSelectedItem();

        if (filePath.isEmpty() || filePath.equals("No file selected...")) {
            showError("Please select a file first.");
            Arrays.fill(password, '\0'); return;
        }
        if (password.length == 0) {
            showError("Please enter a password."); return;
        }
        if (filePath.endsWith(".enc")) {
            int c = JOptionPane.showConfirmDialog(mainFrame,
                "The selected file already has a .enc extension.\n"
                + "It may already be encrypted. Encrypting it again\n"
                + "will create a .enc.enc file. Continue?",
                "File May Already Be Encrypted",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) { Arrays.fill(password, '\0'); return; }
        }

        // Password strength gate — STRONG requires 8+ chars, special char, AND digit
        PasswordStrength ps = evaluateStrength(password);
        if (ps != PasswordStrength.STRONG) {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                "Password is " + ps.name() + "!\n\n"
                + "A STRONG password requires ALL THREE:\n"
                + "  \u00b7 At least 8 characters\n"
                + "  \u00b7 At least one special character  ( @ # $ ! % & * )\n"
                + "  \u00b7 At least one digit  ( 0\u20139 )\n\n"
                + "Continue anyway with a " + ps.name().toLowerCase() + " password?",
                ps.name() + " Password Warning",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) { Arrays.fill(password, '\0'); return; }
        }

        try {
            progressBar.setValue(0); progressBar.setString("Encrypting...");
            statusLabel.setText("Encrypting " + new File(filePath).getName() + "...");

            orchestrator.onEncrypt(filePath, password, algo);

            progressBar.setValue(100); progressBar.setString("Done!");
            statusLabel.setText("Encrypted: " + new File(filePath).getName() + ".enc");
            filePathField.setText("No file selected...");
            refreshLogDisplay();
            JOptionPane.showMessageDialog(mainFrame,
                "File encrypted successfully!\nSaved as: " + filePath + ".enc",
                "Encryption Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showError("Encryption failed:\n" + e.getMessage());
            statusLabel.setText("Encryption failed.");
            progressBar.setValue(0); progressBar.setString("Error");
        } finally {
            Arrays.fill(password, '\0'); // memory security — zero password char[]
            passwordField.setText("");
        }
    }

    private void handleDecrypt() {
        String filePath = filePathField.getText().trim();
        char[] password = passwordField.getPassword();
        String algo     = (String) algorithmDropdown.getSelectedItem();

        if (filePath.isEmpty() || filePath.equals("No file selected...")) {
            showError("Please select a .enc file to decrypt.");
            Arrays.fill(password, '\0'); return;
        }
        if (!filePath.endsWith(".enc")) {
            showError("Please select an encrypted .enc file.\n"
                + "Encrypted files always end with the .enc extension.");
            Arrays.fill(password, '\0'); return;
        }
        if (password.length == 0) {
            showError("Please enter the password used to encrypt this file."); return;
        }

        try {
            progressBar.setValue(0); progressBar.setString("Decrypting...");
            statusLabel.setText("Decrypting...");

            orchestrator.onDecrypt(filePath, password, algo);

            String restored = filePath.substring(0, filePath.length() - 4);
            progressBar.setValue(100); progressBar.setString("Done!");
            statusLabel.setText("Decrypted: " + new File(restored).getName());
            filePathField.setText("No file selected...");
            refreshLogDisplay();
            JOptionPane.showMessageDialog(mainFrame,
                "File decrypted successfully!\nRestored to: " + restored,
                "Decryption Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("integrity") || msg.contains("FAILED")
                    || msg.contains("password") || msg.contains("tampered"))) {
                msg += "\n\nTip: Ensure you used the SAME algorithm (AES-256 or XOR)\n"
                    + "that was selected when this file was originally encrypted.";
            }
            showError("Decryption failed:\n" + msg);
            statusLabel.setText("Decryption failed.");
            progressBar.setValue(0); progressBar.setString("Error");
        } finally {
            Arrays.fill(password, '\0'); // memory security — zero password char[]
            passwordField.setText("");
        }
    }

    private void handleBatchEncrypt() {
        if (batchFiles.isEmpty()) {
            showWarning("No files queued. Click 'Add Files' first."); return;
        }
        char[] password = passwordField.getPassword();
        if (password.length == 0) {
            showWarning("Please enter a password for batch encryption."); return;
        }
        String algo = (String) algorithmDropdown.getSelectedItem();
        progressBar.setValue(0); progressBar.setString("Batch: 0%");
        statusLabel.setText("Batch encrypting " + batchFiles.size() + " files...");

        // isEncrypt = true — BatchProcessor calls orchestrateEncrypt() per file
        BatchProcessor processor = new BatchProcessor(
            new ArrayList<>(batchFiles), password, algo, progressBar, logTextArea,
            currentUser, true);
        processor.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName())
                    && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                batchFiles.clear(); batchListModel.clear();
                statusLabel.setText("Batch encryption complete!");
                refreshLogDisplay();
            }
        });
        processor.execute();
        passwordField.setText(""); // clear from UI immediately — BatchProcessor owns char[]
    }

    private void handleBatchDecrypt() {
        // Validation: at least one file must be queued
        if (batchFiles.isEmpty()) {
            showWarning("No files queued.\nClick 'Add Files' and select .enc files to decrypt.");
            return;
        }

        // Warn (but do not block) if any queued file is not a .enc file.
        // orchestrateDecrypt() will skip non-.enc files per-file and log each one.
        long nonEncCount = batchFiles.stream()
            .filter(f -> !f.getName().endsWith(".enc"))
            .count();
        if (nonEncCount > 0) {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                nonEncCount + " queued file(s) do not have a .enc extension\n"
                + "and will be skipped during Batch Decrypt.\n\n"
                + "Continue with the remaining files?",
                "Non-.enc Files in Queue",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        // Validation: password is required
        char[] password = passwordField.getPassword();
        if (password.length == 0) {
            showWarning("Please enter the password that was used to encrypt these files.");
            return;
        }

        String algo = (String) algorithmDropdown.getSelectedItem();
        progressBar.setValue(0); progressBar.setString("Batch Decrypt: 0%");
        statusLabel.setText("Batch decrypting " + batchFiles.size() + " file(s)...");

        // isEncrypt = false — BatchProcessor calls orchestrateDecrypt() per file
        BatchProcessor processor = new BatchProcessor(
            new ArrayList<>(batchFiles), password, algo, progressBar, logTextArea,
            currentUser, false);
        processor.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName())
                    && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                batchFiles.clear();
                batchListModel.clear();
                statusLabel.setText("Batch decryption complete.");
                refreshLogDisplay();
            }
        });
        processor.execute();
        // Clear the password field from the UI immediately.
        // BatchProcessor now owns the char[] and will zero it inside done().
        passwordField.setText("");
    }

    private void handleEscrowRecovery() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty() || filePath.equals("No file selected...")) {
            showWarning("Select a .enc file for Key Escrow recovery."); return;
        }
        if (!filePath.endsWith(".enc")) {
            showWarning("Key Escrow only works on AES-256 encrypted .enc files."); return;
        }
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
            "Use Admin Key Escrow to recover this file?\n\n"
            + "File: " + filePath + "\n\n"
            + "This decrypts using the admin's master key.\n"
            + "No user password is required.\n"
            + "(Session key unwrapped from AES header bytes 16-63)",
            "Confirm Key Escrow Recovery", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                orchestrator.onEscrowRecovery(filePath);
                filePathField.setText("No file selected...");
                statusLabel.setText("Key Escrow recovery complete.");
                refreshLogDisplay();
                JOptionPane.showMessageDialog(mainFrame,
                    "Key Escrow recovery successful!",
                    "Recovery Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                showError("Key Escrow recovery failed:\n" + e.getMessage());
            }
        }
    }

    private void handleClearLog() {
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
            "Clear the entire activity log?\nThis cannot be undone.",
            "Clear Log", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            ActivityLogger.getInstance().clearLog();
            refreshLogDisplay();
            statusLabel.setText("Activity log cleared.");
        }
    }

    // -----------------------------------------------------------------------
    // PASSWORD STRENGTH
    // STRONG = ALL THREE: length >= 8, has special char, has digit.
    // AVERAGE = length OK but one extra condition missing.
    // WEAK = length < 8 OR neither extra condition met.
    // -----------------------------------------------------------------------
    private enum PasswordStrength { WEAK, AVERAGE, STRONG }

    private PasswordStrength evaluateStrength(char[] pw) {
        if (pw.length == 0) return PasswordStrength.WEAK;
        boolean hasSpecial = false;
        boolean hasDigit   = false;
        for (char c : pw) {
            if (Character.isDigit(c))          hasDigit   = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        if (pw.length >= 8 && hasSpecial && hasDigit) return PasswordStrength.STRONG;
        if (pw.length >= 8 && (hasSpecial || hasDigit)) return PasswordStrength.AVERAGE;
        return PasswordStrength.WEAK;
    }

    private void updateStrength() {
        char[] pw = passwordField.getPassword();
        PasswordStrength ps = evaluateStrength(pw);
        Arrays.fill(pw, '\0'); // memory security — zero immediately, before any branching

        // pw.length is the original array length — Arrays.fill does not change it.
        if (pw.length == 0) {
            strengthLabel.setText("  Strength: ---");
            strengthLabel.setForeground(CLR_GRAY);
            return;
        }
        switch (ps) {
            case WEAK:
                strengthLabel.setText("  Strength: WEAK  (need 8+ chars + special char + digit)");
                strengthLabel.setForeground(CLR_RED);
                break;
            case AVERAGE:
                strengthLabel.setText("  Strength: AVERAGE  (add a digit or special char)");
                strengthLabel.setForeground(CLR_YELLOW);
                break;
            case STRONG:
                strengthLabel.setText("  Strength: STRONG  \u2713");
                strengthLabel.setForeground(CLR_GREEN);
                break;
        }
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private void refreshLogDisplay() {
        String content = ActivityLogger.getInstance().getLogContents();
        logTextArea.setText(content);
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Warning",
                                      JOptionPane.WARNING_MESSAGE);
    }

    // ── Swing component factories ──

    private JTextField makeDarkTextField() {
        JTextField f = new JTextField();
        f.setBackground(BG_FIELD);
        f.setForeground(CLR_TEXT);
        f.setCaretColor(CLR_GREEN);
        f.setFont(FONT_MONO);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CLR_BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return f;
    }

    private void stylePasswordField(JPasswordField f) {
        f.setBackground(BG_FIELD);
        f.setForeground(CLR_TEXT);
        f.setCaretColor(CLR_GREEN);
        f.setEchoChar('\u2022');
        f.setFont(FONT_MONO);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CLR_BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
    }

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_MONO);
        lbl.setForeground(CLR_GRAY);
        lbl.setPreferredSize(new Dimension(72, 28));
        return lbl;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Consolas", Font.BOLD, 11));
        lbl.setForeground(CLR_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        return lbl;
    }

    private JButton makeButton(String text, Color accentColor) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_FIELD);
        btn.setForeground(accentColor);
        btn.setFont(FONT_BOLD);
        btn.setBorder(BorderFactory.createLineBorder(accentColor, 1));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(accentColor); btn.setForeground(BG_DARK); }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_FIELD); btn.setForeground(accentColor); }
        });
        return btn;
    }

    private JButton makeLaunchButton(String label, Color color, String caption) {
        JButton btn = new JButton(
            "<html><center>"
            + "<b style='font-size:15px;'>" + label + "</b><br>"
            + "<span style='font-size:9px;color:#aaa;'>" + caption + "</span>"
            + "</center></html>");
        btn.setBackground(BG_CARD);
        btn.setForeground(color);
        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(18, 44, 18, 44)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(260, 90));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(color); btn.setForeground(BG_DARK); }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_CARD); btn.setForeground(color); }
        });
        return btn;
    }

    private JButton makeDriveButton() {
        JButton btn = new JButton("  \u2601  Sync to Google Drive");
        btn.setBackground(BG_FIELD);
        btn.setForeground(CLR_DRIVE);
        btn.setFont(FONT_BOLD);
        btn.setBorder(BorderFactory.createLineBorder(CLR_DRIVE, 1));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(CLR_DRIVE); btn.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_FIELD); btn.setForeground(CLR_DRIVE); }
        });
        return btn;
    }

    private Component vGap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}