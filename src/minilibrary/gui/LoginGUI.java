package minilibrary.gui;

import minilibrary.database.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

// ─────────────────────────────────────────────
//  LOGIN GUI
//  First screen the user sees on launch.
//  Credentials are verified against the
//  users table in the mini_library database.
//  On success, disposes itself and opens MainAppGUI.
// ─────────────────────────────────────────────
public class LoginGUI {

    private final JFrame       frame;
    private JTextField         usernameField;
    private JPasswordField     passwordField;
    private final JLabel       statusLabel;

    public LoginGUI() {
        frame = new JFrame("Mini-Library Accounts Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.setSize(1000, 540);
        frame.setLocationRelativeTo(null);

        // ── Center panel ──────────────────────
        JPanel center = new JPanel(new GridBagLayout());
        center.setBorder(BorderFactory.createEmptyBorder(40, 160, 40, 160));
        center.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // Header
        JLabel header = new JLabel("Welcome to the Mini-Library!", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(new Color(40, 40, 40));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        center.add(header, gbc);

        gbc.gridy++;
        center.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        // Username
        gbc.gridwidth = 1; gbc.gridy++;
        gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        center.add(userLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setColumns(20);
        center.add(usernameField, gbc);

        // Password
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        center.add(passLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setColumns(20);
        center.add(passwordField, gbc);

        // Status label
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        center.add(statusLabel, gbc);

        // Buttons
        gbc.gridy++; gbc.insets = new Insets(18, 8, 0, 8);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setBackground(Color.WHITE);
        JButton loginBtn = makeFlatBtn("Log In", new Color(46, 204, 113));
        JButton clearBtn = makeFlatBtn("Clear",  new Color(231, 76, 60));
        buttons.add(loginBtn); buttons.add(clearBtn);
        center.add(buttons, gbc);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(250, 250, 250));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        JLabel foot = new JLabel("Enter your account details.");
        foot.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        foot.setForeground(Color.GRAY);
        footer.add(foot);

        frame.add(center, BorderLayout.CENTER);
        frame.add(footer,  BorderLayout.SOUTH);
        frame.getRootPane().setDefaultButton(loginBtn);

        // ESC clears the fields
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
        frame.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearFields(); }
        });

        loginBtn.addActionListener(e -> { loginBtn.setEnabled(false); doLogin(); loginBtn.setEnabled(true); });
        clearBtn.addActionListener(e -> clearFields());
    }

    /** Makes the login window visible. */
    public void show() { SwingUtilities.invokeLater(() -> frame.setVisible(true)); }

    // ─────────────────────────────────────────
    //  LOGIN FLOW
    // ─────────────────────────────────────────

    /** Validates inputs, queries the DB, and launches MainAppGUI on success. */
    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        // Basic empty-field guard
        if (u.isEmpty() || p.isEmpty()) {
            setStatus("Please enter both fields.", Color.RED);
            return;
        }

        try {
            // authenticateFromDB throws SQLException so we can tell the difference
            // between "can't reach DB" and "wrong credentials"
            boolean ok = authenticateFromDB(u, p);
            if (ok) {
                setStatus("Login successful. Launching…", new Color(0, 128, 0));
                SwingUtilities.invokeLater(() -> { frame.dispose(); new MainAppGUI().show(); });
            } else {
                // Connected to DB fine, but credentials didn't match
                setStatus("Invalid username or password.", Color.RED);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }

        } catch (SQLException ex) {
            // Connection problem – show a clear error instead of "wrong password"
            ex.printStackTrace();
            setStatus("DB error: " + ex.getMessage(), Color.RED);
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to the database.\n\n"
                    + "Check that:\n"
                    + "  1. MySQL Server is running\n"
                    + "  2. The password in DBConnection.java matches your root password\n"
                    + "  3. The mini_library database exists\n\n"
                    + "Error: " + ex.getMessage(),
                    "Database Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Queries the users table to verify the supplied credentials.
     *
     * @throws SQLException if the connection to MySQL itself fails, so the
     *         caller can show a "can't connect" error instead of silently
     *         returning false and looking like a wrong password.
     */
    private boolean authenticateFromDB(String username, String password)
            throws SQLException {

        String sql = "SELECT password FROM users WHERE username = ?";

        try (java.sql.PreparedStatement ps =
                     DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    return stored.equals(password); // plain-text comparison
                }
                return false; // username not found
            }
        }
        // Any SQLException bubbles up to doLogin()
    }

    // ─────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────
    private void clearFields() {
        usernameField.setText(""); passwordField.setText("");
        setStatus(" ", Color.BLACK); usernameField.requestFocusInWindow();
    }
    private void setStatus(String t, Color c) { statusLabel.setText(t); statusLabel.setForeground(c); }
    private JButton makeFlatBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(110, 36)); b.setOpaque(true);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }
}
