package src;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// ─────────────────────────────────────────────
//  ENTRY POINT
// ─────────────────────────────────────────────
public class Exer6_MiniLibrary {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginGUI().show());
    }
}

// ─────────────────────────────────────────────
//  MODEL: TempBook  (Regular Book)
// ─────────────────────────────────────────────
class TempBook {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);
    private final int id;
    private String title;
    private int    pages;

    /** Normal constructor – assigns a new auto-increment ID. */
    public TempBook()              { this.id = ID_GEN.getAndIncrement(); }
    /** Edit constructor – reuses an existing ID. */
    public TempBook(int existingId){ this.id = existingId; }

    /**
     * Called once after INSERT to sync the MySQL auto-generated ID
     * back into this object. Uses reflection because the id field is final.
     */
    public void setDbId(int dbId) {
        try {
            java.lang.reflect.Field f = TempBook.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(this, dbId);
        } catch (Exception ignored) {}
    }

    public int    getId()    { return id; }
    public String getTitle() { return title; }
    public int    getPages() { return pages; }
    public String getType()  { return "Regular Book"; }

    public void setTitle(String t) {
        if (t == null || t.isBlank()) throw new IllegalArgumentException("Title cannot be empty.");
        this.title = t.trim();
    }
    public void setPages(int p) {
        if (p <= 0) throw new IllegalArgumentException("Pages must be a positive number.");
        this.pages = p;
    }

    @Override public String toString() {
        return String.format("[%d] %s | %s | %d pages", id, title, getType(), pages);
    }
}

// ─────────────────────────────────────────────
//  MODEL: TempText  (Textbook)
// ─────────────────────────────────────────────
class TempText extends TempBook {
    private int gradeLevel;

    public TempText()               { super(); }
    public TempText(int existingId) { super(existingId); }

    public int  getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(int g) {
        if (g < 1 || g > 13) throw new IllegalArgumentException("Grade level must be 1 – 13.");
        this.gradeLevel = g;
    }

    @Override public String getType() { return "Textbook"; }

    @Override public String toString() {
        return String.format("[%d] %s | %s (Grade %d) | %d pages",
                getId(), getTitle(), getType(), gradeLevel, getPages());
    }
}

// ─────────────────────────────────────────────
//  DATABASE CONNECTION  (MySQL via JDBC)
// ─────────────────────────────────────────────
class DBConnection {
    // ── Change these three values to match your DataBase setup ──
    private static final String URL      = "jdbc:mysql://localhost:3306/mini_library";
    private static final String USER     = "root";
    private static final String PASSWORD = " "; // ← Your database password here

    private static java.sql.Connection conn = null;

    /**
     * Returns a live connection, creating one if needed.
     * Throws SQLException so callers can distinguish a connection
     * failure from a bad username/password in the users table.
     */
    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✔ MySQL connected.");
            }
        } catch (ClassNotFoundException e) {
            // Driver JAR is missing from lib/ – throw as SQLException so the UI can catch it
            throw new java.sql.SQLException("MySQL JDBC Driver not found. Check your lib/ folder.", e);
        }
        return conn; // DriverManager.getConnection already throws SQLException on bad credentials
    }

    /** Call this when the app closes to release the connection cleanly. */
    public static void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (Exception ignored) {}
    }
}

// ─────────────────────────────────────────────
//  BOOK DATABASE  (MySQL-backed CRUD)
// ─────────────────────────────────────────────
class BookDatabase {
    private static BookDatabase instance;
    public  static BookDatabase getInstance() {
        if (instance == null) instance = new BookDatabase();
        return instance;
    }

    // ── CREATE ────────────────────────────────
    public void add(TempBook book) {
        String sql = "INSERT INTO books (type, title, pages, grade_level) VALUES (?, ?, ?, ?)";
        try (java.sql.PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getType());
            ps.setString(2, book.getTitle());
            ps.setInt   (3, book.getPages());
            // grade_level is NULL for Regular Books, filled for Textbooks
            if (book instanceof TempText tb) ps.setInt(4, tb.getGradeLevel());
            else                             ps.setNull(4, java.sql.Types.INTEGER);
            ps.executeUpdate();

            // Sync the MySQL auto-generated ID back into the Java object
            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setDbId(keys.getInt(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── READ (all) ────────────────────────────
    public java.util.List<TempBook> getAll() {
        java.util.List<TempBook> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY id";
        try (java.sql.Statement st  = DBConnection.getConnection().createStatement();
             java.sql.ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                int    id    = rs.getInt("id");
                String type  = rs.getString("type");
                String title = rs.getString("title");
                int    pages = rs.getInt("pages");
                int    grade = rs.getInt("grade_level"); // returns 0 if NULL — safe to ignore for books

                TempBook book;
                if ("Textbook".equals(type)) {
                    TempText tb = new TempText(id);
                    tb.setTitle(title); tb.setPages(pages); tb.setGradeLevel(grade);
                    book = tb;
                } else {
                    book = new TempBook(id);
                    book.setTitle(title); book.setPages(pages);
                }
                list.add(book);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ── READ (by ID) ──────────────────────────
    public java.util.Optional<TempBook> findById(int id) {
        return getAll().stream().filter(b -> b.getId() == id).findFirst();
    }

    // ── UPDATE ────────────────────────────────
    public boolean update(TempBook updated) {
        String sql = "UPDATE books SET type=?, title=?, pages=?, grade_level=? WHERE id=?";
        try (java.sql.PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, updated.getType());
            ps.setString(2, updated.getTitle());
            ps.setInt   (3, updated.getPages());
            if (updated instanceof TempText tb) ps.setInt(4, tb.getGradeLevel());
            else                                ps.setNull(4, java.sql.Types.INTEGER);
            ps.setInt(5, updated.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── DELETE ────────────────────────────────
    public boolean delete(int id) {
        String sql = "DELETE FROM books WHERE id=?";
        try (java.sql.PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}

// ─────────────────────────────────────────────
//  LOGIN GUI
// ─────────────────────────────────────────────
class LoginGUI {
    private final JFrame       frame;
    private JTextField         usernameField;
    private JPasswordField     passwordField;
    private final JLabel       statusLabel;

    // ── demoCredentials map REMOVED – login now queries the users table in MySQL ──

    public LoginGUI() {
        frame = new JFrame("Mini-Library Accounts Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.setSize(1000, 540);
        frame.setLocationRelativeTo(null);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBorder(BorderFactory.createEmptyBorder(40, 160, 40, 160));
        center.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

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

        // Status
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

        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
        frame.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearFields(); }
        });

        loginBtn.addActionListener(e -> { loginBtn.setEnabled(false); doLogin(); loginBtn.setEnabled(true); });
        clearBtn.addActionListener(e -> clearFields());
    }

    public void show() { SwingUtilities.invokeLater(() -> frame.setVisible(true)); }

    // ─────────────────────────────────────────
    //  LOGIN FLOW
    // ─────────────────────────────────────────
    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        // Basic empty-field guard
        if (u.isEmpty() || p.isEmpty()) {
            setStatus("Please enter both fields.", Color.RED);
            return;
        }

        try {
            // authenticateFromDB now throws SQLException so we can tell the
            // difference between "can't reach DB" and "wrong credentials"
            boolean ok = authenticateFromDB(u, p);
            if (ok) {
                setStatus("Login successful. Launching…", new Color(0, 128, 0));
                SwingUtilities.invokeLater(() -> { frame.dispose(); new MainAppGUI().show(); });
            } else {
                // Username found but password didn't match
                setStatus("Invalid username or password.", Color.RED);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }

        } catch (java.sql.SQLException ex) {
            // ── Connection problem – show a clear message instead of "wrong password" ──
            // Print the full stack trace to the console for debugging
            ex.printStackTrace();
            setStatus("DB error: " + ex.getMessage(), Color.RED);
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to the database.\n\n"
                    + "Check that:\n"
                    + "  1. MySQL Server is running\n"
                    + "  2. The password in DBConnection matches your root password\n"
                    + "  3. The mini_library database exists\n\n"
                    + "Error: " + ex.getMessage(),
                    "Database Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Queries the users table to verify the supplied credentials.
     *
     * @throws java.sql.SQLException if the connection to MySQL itself fails,
     *         so the caller can show a proper "can't connect" error instead
     *         of silently returning false and showing "wrong password".
     */
    private boolean authenticateFromDB(String username, String password)
            throws java.sql.SQLException {

        String sql = "SELECT password FROM users WHERE username = ?";

        // getConnection() now throws SQLException on failure – no more silent null returns
        try (java.sql.PreparedStatement ps =
                     DBConnection.getConnection().prepareStatement(sql)) {

            ps.setString(1, username);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    return stored.equals(password); // plain-text comparison
                }
                // Username not found in the table → return false (wrong credentials)
                return false;
            }
        }
        // Any SQLException (connection lost, query error, etc.) bubbles up to doLogin()
    }

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

// ─────────────────────────────────────────────
//  MAIN APP GUI  –  Full CRUD with Split Tables
// ─────────────────────────────────────────────
class MainAppGUI {

    // ── Database ──────────────────────────────
    private final BookDatabase db = BookDatabase.getInstance();

    // ── Edit state ────────────────────────────
    private Integer editingId = null;   // null = ADD mode

    // ── Frame ─────────────────────────────────
    private final JFrame frame;

    // ── Form widgets ──────────────────────────
    private JComboBox<String> cmbType;
    private JTextField        txtTitle, txtPages, txtGradeLevel;
    private JLabel            statusLabel, formHeader;

    // ── CRUD buttons (all centralised on the form panel) ──────────
    // btnSave / btnClear  → always visible (Add / Update / reset)
    // btnEdit / btnDelete → start disabled; enabled only when a row is selected
    private JButton btnSave, btnClear, btnEdit, btnDelete;

    // ── Tables ────────────────────────────────
    private JTable            tblBooks, tblTextbooks;
    private DefaultTableModel mdlBooks, mdlTextbooks;
    private JLabel            cntBooks, cntTextbooks;

    // Column layout shared by both tables
    private static final String[] BOOK_COLS = {"ID", "Title", "Pages"};
    private static final String[] TEXT_COLS = {"ID", "Title", "Pages", "Grade"};
    private static final int      COL_ID    = 0;
    private static final int      COL_TITLE = 1;
    private static final int      COL_PAGES = 2;
    private static final int      COL_GRADE = 3;   // textbook table only

    public MainAppGUI() {
        frame = new JFrame("Mini-Library – Main");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));

        // Split: left = form (with all CRUD buttons), right = tables (display only)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createFormPanel(), createRightPanel());
        split.setDividerLocation(370);
        split.setDividerSize(6);
        split.setBorder(new EmptyBorder(16, 16, 16, 16));
        split.setBackground(Color.WHITE);

        frame.add(split,          BorderLayout.CENTER);
        frame.add(createFooter(), BorderLayout.SOUTH);
        frame.setSize(1100, 600);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);

        // Release the MySQL connection when the window is closed
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                DBConnection.close();
            }
        });

        refreshTables();
    }

    public void show() { SwingUtilities.invokeLater(() -> frame.setVisible(true)); }

    // ══════════════════════════════════════════
    //  LEFT  –  FORM PANEL
    //  Contains all input fields AND all four
    //  CRUD buttons (Save, Cancel, Edit, Delete)
    // ══════════════════════════════════════════
    private JPanel createFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 12, 4, 12);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Form header (dynamic – changes to "Edit Record #N" when editing)
        formHeader = new JLabel("Add New Book", SwingConstants.CENTER);
        formHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formHeader.setForeground(new Color(40, 40, 40));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        p.add(formHeader, g);

        // Separator
        g.gridy++;
        p.add(makeSeparator(), g);

        // ── Type ──
        g.gridy++; g.gridwidth = 1;
        g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(lbl("Item Type:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        cmbType = new JComboBox<>(new String[]{"Regular Book", "Textbook"});
        cmbType.setBackground(Color.WHITE);
        cmbType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbType.addActionListener(e -> onTypeChange());
        p.add(cmbType, g);

        // ── Title ──
        g.gridy++; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(lbl("Item Title:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        txtTitle = field(); p.add(txtTitle, g);

        // ── Pages ──
        g.gridy++; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(lbl("Number of Pages:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        txtPages = field(); p.add(txtPages, g);

        // ── Grade Level (only enabled when Textbook is selected) ──
        g.gridy++; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(lbl("Grade Level:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        txtGradeLevel = field();
        txtGradeLevel.setEnabled(false);
        txtGradeLevel.setBackground(new Color(245, 245, 245));
        p.add(txtGradeLevel, g);

        // ── Status label (validation errors and success messages) ──
        g.gridy++; g.gridx = 0; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(statusLabel, g);

        // ── ROW 1: Save + Cancel ──────────────────────────────────
        // These two buttons handle CREATE (Save) and resetting the form (Cancel).
        // When in edit mode, Save becomes "Update".
        g.gridy++; g.insets = new Insets(14, 12, 4, 12);
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        saveRow.setBackground(Color.WHITE);
        btnSave  = flatBtn("Save",   new Color(46, 204, 113));   // green – confirm action
        btnClear = flatBtn("Cancel", new Color(149, 165, 166));  // grey  – discard / reset
        btnSave.addActionListener (e -> onSave());
        btnClear.addActionListener(e -> onClear());
        saveRow.add(btnSave);
        saveRow.add(btnClear);
        p.add(saveRow, g);

        // ── ROW 2: Edit + Delete ──────────────────────────────────
        // These two buttons act on the currently selected table row.
        // Both start DISABLED – they are only enabled once a row is
        // selected in either table (see the selection listeners in
        // buildTableSection). They are disabled again by onClear().
        g.gridy++; g.insets = new Insets(0, 12, 10, 12);
        JPanel editRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        editRow.setBackground(Color.WHITE);
        btnEdit   = flatBtn("Edit",   new Color(52, 152, 219));  // blue – load record into form
        btnDelete = flatBtn("Delete", new Color(231, 76, 60));   // red  – remove record
        btnEdit  .setEnabled(false);
        btnDelete.setEnabled(false);
        btnEdit.addActionListener(e -> {
            // Find whichever table has an active selection and load that record
            int id = getSelectedId();
            if (id < 0) { showWarn("Please select a record to edit."); return; }
            loadForEdit(id);
        });
        btnDelete.addActionListener(e -> {
            // Find whichever table has an active selection and delete that record
            int id = getSelectedId();
            if (id < 0) { showWarn("Please select a record to delete."); return; }
            confirmDelete(id);
        });
        editRow.add(btnEdit);
        editRow.add(btnDelete);
        p.add(editRow, g);

        // Keyboard shortcuts
        frame.getRootPane().setDefaultButton(btnSave);
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESC");
        frame.getRootPane().getActionMap().put("ESC", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onClear(); }
        });

        g.gridy++; g.weighty = 1.0;
        p.add(Box.createGlue(), g);
        return p;
    }

    // ══════════════════════════════════════════
    //  RIGHT  –  SPLIT TABLES PANEL
    //  Display-only: no buttons here anymore.
    //  Each section shows its own record count.
    // ══════════════════════════════════════════
    private JPanel createRightPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 12));
        p.setBackground(Color.WHITE);
        p.add(buildTableSection("Regular Books", BOOK_COLS, true));
        p.add(buildTableSection("Textbooks",     TEXT_COLS, false));
        return p;
    }

    /**
     * Builds one table section: a titled header, a scrollable JTable, and a
     * slim footer showing the record count. Edit / Delete buttons have been
     * moved to the form panel, so this section is now display-only.
     *
     * @param title   Section heading text
     * @param cols    Column names for this table
     * @param isBooks true → Regular Book table; false → Textbook table
     */
    private JPanel buildTableSection(String title, String[] cols, boolean isBooks) {
        JPanel section = new JPanel(new BorderLayout(0, 0));
        section.setBackground(Color.WHITE);
        section.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));

        // ── Section header ──
        JLabel hdr = new JLabel("  " + title, SwingConstants.LEFT);
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hdr.setForeground(new Color(50, 50, 50));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(245, 247, 250));
        hdr.setBorder(new EmptyBorder(8, 10, 8, 10));
        section.add(hdr, BorderLayout.NORTH);

        // ── Table ──
        DefaultTableModel mdl = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(mdl);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.setRowHeight(24);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tbl.getTableHeader().setBackground(new Color(236, 240, 241));
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.setGridColor(new Color(230, 230, 230));
        tbl.setShowGrid(true);
        tbl.setRowSelectionAllowed(true);
        // Keep the ID column narrow – it's just a reference, not main content
        tbl.getColumnModel().getColumn(COL_ID).setPreferredWidth(35);
        tbl.getColumnModel().getColumn(COL_ID).setMaxWidth(50);

        // ── Store table / model references so the form panel can reach them ──
        if (isBooks) { tblBooks = tbl; mdlBooks = mdl; }
        else         { tblTextbooks = tbl; mdlTextbooks = mdl; }

        // ── Selection listener ─────────────────────────────────────────────
        // When a row is selected in this table:
        //   1. Clear any selection in the OTHER table (only one row active at a time)
        //   2. Enable the Edit and Delete buttons on the form panel
        // When the selection is cleared (row == -1), disable both buttons again.
        tbl.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean hasSelection = tbl.getSelectedRow() >= 0;
            if (hasSelection) {
                // Deselect the other table so there is never an ambiguous selection
                if (isBooks  && tblTextbooks != null) tblTextbooks.clearSelection();
                if (!isBooks && tblBooks     != null) tblBooks.clearSelection();
            }
            // Enable / disable the form-panel buttons based on whether a row is selected
            if (btnEdit   != null) btnEdit  .setEnabled(hasSelection);
            if (btnDelete != null) btnDelete.setEnabled(hasSelection);
        });

        section.add(new JScrollPane(tbl), BorderLayout.CENTER);

        // ── Footer – record count only (buttons moved to form panel) ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        footer.setBackground(new Color(250, 251, 252));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        JLabel cnt = new JLabel("0 record(s)");
        cnt.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        cnt.setForeground(Color.GRAY);
        if (isBooks) cntBooks = cnt; else cntTextbooks = cnt;
        footer.add(cnt);
        section.add(footer, BorderLayout.SOUTH);

        return section;
    }

    private JPanel createFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.CENTER));
        f.setBackground(new Color(250, 250, 250));
        f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        // Updated hint to reflect the new button layout
        JLabel hint = new JLabel(
            "Select a row to enable Edit / Delete on the left panel  ·  ESC cancels editing");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        f.add(hint);
        return f;
    }

    // ══════════════════════════════════════════
    //  CRUD ACTIONS
    // ══════════════════════════════════════════

    /** CREATE or UPDATE depending on editingId. */
    private void onSave() {
        statusLabel.setText(" ");
        try {
            String  title    = txtTitle.getText().trim();
            String  pagesStr = txtPages.getText().trim();
            String  gradeStr = txtGradeLevel.getText().trim();
            boolean isText   = "Textbook".equals(cmbType.getSelectedItem());

            // Validation
            List<String> missing = new ArrayList<>();
            if (title.isEmpty())              missing.add("Title");
            if (pagesStr.isEmpty())           missing.add("Number of Pages");
            if (isText && gradeStr.isEmpty()) missing.add("Grade Level");
            if (!missing.isEmpty()) {
                Toolkit.getDefaultToolkit().beep();
                setStatus(buildMsg(missing), new Color(231, 76, 60));
                return;
            }

            int pages = Integer.parseInt(pagesStr);

            if (editingId == null) {
                // ── CREATE ──────────────────────────────
                TempBook book;
                if (isText) {
                    TempText tb = new TempText();
                    tb.setTitle(title); tb.setPages(pages);
                    tb.setGradeLevel(Integer.parseInt(gradeStr));
                    book = tb;
                } else {
                    book = new TempBook();
                    book.setTitle(title); book.setPages(pages);
                }
                db.add(book);
                setStatus("Record #" + book.getId() + " saved!", new Color(39, 174, 96));

            } else {
                // ── UPDATE ──────────────────────────────
                TempBook updated;
                if (isText) {
                    TempText tb = new TempText(editingId);
                    tb.setTitle(title); tb.setPages(pages);
                    tb.setGradeLevel(Integer.parseInt(gradeStr));
                    updated = tb;
                } else {
                    updated = new TempBook(editingId);
                    updated.setTitle(title); updated.setPages(pages);
                }
                db.update(updated);
                setStatus("Record #" + editingId + " updated!", new Color(52, 152, 219));
            }

            refreshTables();
            onClear();

        } catch (NumberFormatException ex) {
            Toolkit.getDefaultToolkit().beep();
            setStatus("Pages and Grade must be valid numbers.", new Color(231, 76, 60));
        } catch (IllegalArgumentException ex) {
            Toolkit.getDefaultToolkit().beep();
            setStatus(ex.getMessage(), new Color(231, 76, 60));
        }
    }

    /** Load a record into the form for editing (called by Edit button). */
    private void loadForEdit(int id) {
        db.findById(id).ifPresent(book -> {
            editingId = id;
            txtTitle.setText(book.getTitle());
            txtPages.setText(String.valueOf(book.getPages()));
            if (book instanceof TempText tb) {
                cmbType.setSelectedItem("Textbook");
                txtGradeLevel.setText(String.valueOf(tb.getGradeLevel()));
            } else {
                cmbType.setSelectedItem("Regular Book");
                txtGradeLevel.setText("");
            }
            onTypeChange();
            btnSave.setText("Update");
            formHeader.setText("Edit  Record  #" + id);
            statusLabel.setText(" ");
            txtTitle.requestFocus();
        });
    }

    /** Show confirm dialog then delete (called by Delete button). */
    private void confirmDelete(int id) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Delete Record #" + id + "?\nThis cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            db.delete(id);
            refreshTables();
            if (editingId != null && editingId == id) onClear();
            setStatus("Record #" + id + " deleted.", new Color(192, 57, 43));
        }
    }

    /**
     * Returns the ID of the currently selected row across both tables.
     * Checks the Regular Books table first, then Textbooks.
     * Returns -1 if nothing is selected in either table.
     */
    private int getSelectedId() {
        int row = tblBooks.getSelectedRow();
        if (row >= 0) return (int) mdlBooks.getValueAt(row, COL_ID);
        row = tblTextbooks.getSelectedRow();
        if (row >= 0) return (int) mdlTextbooks.getValueAt(row, COL_ID);
        return -1;
    }

    /** Reset form to ADD mode and disable the Edit / Delete buttons. */
    private void onClear() {
        editingId = null;
        txtTitle.setText(""); txtPages.setText(""); txtGradeLevel.setText("");
        cmbType.setSelectedIndex(0);
        onTypeChange();
        btnSave.setText("Save");
        formHeader.setText("Add New Book");
        // Clear table selections – this also triggers the listener which disables Edit/Delete
        tblBooks.clearSelection();
        tblTextbooks.clearSelection();
        txtTitle.requestFocus();
    }

    /** Enable / disable Grade Level field when type changes. */
    private void onTypeChange() {
        boolean isText = "Textbook".equals(cmbType.getSelectedItem());
        txtGradeLevel.setEnabled(isText);
        txtGradeLevel.setBackground(isText ? Color.WHITE : new Color(245, 245, 245));
        if (!isText) txtGradeLevel.setText("");
    }

    // ══════════════════════════════════════════
    //  TABLE REFRESH  (READ)
    // ══════════════════════════════════════════
    private void refreshTables() {
        mdlBooks.setRowCount(0);
        mdlTextbooks.setRowCount(0);
        int books = 0, texts = 0;
        for (TempBook b : db.getAll()) {
            if (b instanceof TempText tb) {
                mdlTextbooks.addRow(new Object[]{
                        tb.getId(), tb.getTitle(), tb.getPages(), tb.getGradeLevel()});
                texts++;
            } else {
                mdlBooks.addRow(new Object[]{b.getId(), b.getTitle(), b.getPages()});
                books++;
            }
        }
        cntBooks.setText(books + " record(s)");
        cntTextbooks.setText(texts + " record(s)");
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════
    private void setStatus(String msg, Color c) { statusLabel.setText(msg); statusLabel.setForeground(c); }
    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "No Selection", JOptionPane.WARNING_MESSAGE);
    }
    private String buildMsg(List<String> miss) {
        int n = miss.size();
        if (n == 1) return miss.get(0) + " is required.";
        if (n == 2) return miss.get(0) + " and " + miss.get(1) + " are required.";
        return String.join(", ", miss.subList(0, n - 1)) + ", and " + miss.get(n - 1) + " are required.";
    }
    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 220, 220));
        return sep;
    }
    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return l;
    }
    private JTextField field() {
        JTextField tf = new JTextField(15);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return tf;
    }
    private JButton flatBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(100, 34)); b.setOpaque(true);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }
    private JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setPreferredSize(new Dimension(90, 26)); b.setOpaque(true);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }
}