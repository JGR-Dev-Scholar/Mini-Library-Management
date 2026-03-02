package minilibrary.gui;

import minilibrary.database.BookDatabase;
import minilibrary.database.DBConnection;
import minilibrary.model.TempBook;
import minilibrary.model.TempText;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────
//  MAIN APP GUI  –  Full CRUD with Split Tables
//  Left panel  → input form + all four CRUD buttons
//  Right panel → two separate read-only tables
//                (Regular Books / Textbooks)
// ─────────────────────────────────────────────
public class MainAppGUI {

    // ── Database reference ────────────────────
    private final BookDatabase db = BookDatabase.getInstance();

    // ── Edit state ────────────────────────────
    // null  = ADD mode  (Save creates a new record)
    // non-null = EDIT mode (Save updates the record with this ID)
    private Integer editingId = null;

    // ── Frame ─────────────────────────────────
    private final JFrame frame;

    // ── Form widgets ──────────────────────────
    private JComboBox<String> cmbType;
    private JTextField        txtTitle, txtPages, txtGradeLevel;
    private JLabel            statusLabel, formHeader;

    // ── CRUD buttons (all centralised on the form panel) ──────────
    // btnSave / btnClear  → always visible (Add / Update / reset)
    // btnEdit / btnDelete → start DISABLED; enabled only when a table row is selected
    private JButton btnSave, btnClear, btnEdit, btnDelete;

    // ── Tables ────────────────────────────────
    private JTable            tblBooks, tblTextbooks;
    private DefaultTableModel mdlBooks, mdlTextbooks;
    private JLabel            cntBooks, cntTextbooks;

    // ── Column indices ────────────────────────
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
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { DBConnection.close(); }
        });

        // Load existing records from MySQL when the app first opens
        refreshTables();
    }

    /** Makes the main window visible. */
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

        // Separator line below header
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
        // Save  → CREATE a new record (or UPDATE if in edit mode)
        // Cancel → reset form back to ADD mode
        g.gridy++; g.insets = new Insets(14, 12, 4, 12);
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        saveRow.setBackground(Color.WHITE);
        btnSave  = flatBtn("Save",   new Color(46, 204, 113));   // green
        btnClear = flatBtn("Cancel", new Color(149, 165, 166));  // grey
        btnSave.addActionListener (e -> onSave());
        btnClear.addActionListener(e -> onClear());
        saveRow.add(btnSave); saveRow.add(btnClear);
        p.add(saveRow, g);

        // ── ROW 2: Edit + Delete ──────────────────────────────────
        // Both start DISABLED – enabled when a table row is selected
        // (see selection listeners in buildTableSection).
        // Disabled again when onClear() is called.
        g.gridy++; g.insets = new Insets(0, 12, 10, 12);
        JPanel editRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        editRow.setBackground(Color.WHITE);
        btnEdit   = flatBtn("Edit",   new Color(52, 152, 219));  // blue
        btnDelete = flatBtn("Delete", new Color(231, 76, 60));   // red
        btnEdit  .setEnabled(false);
        btnDelete.setEnabled(false);
        btnEdit.addActionListener(e -> {
            int id = getSelectedId();
            if (id < 0) { showWarn("Please select a record to edit."); return; }
            loadForEdit(id);
        });
        btnDelete.addActionListener(e -> {
            int id = getSelectedId();
            if (id < 0) { showWarn("Please select a record to delete."); return; }
            confirmDelete(id);
        });
        editRow.add(btnEdit); editRow.add(btnDelete);
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
    //  Display-only: no action buttons here.
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
     * Builds one table section: titled header + scrollable JTable + record count footer.
     * Edit / Delete buttons have been moved to the form panel, so this is display-only.
     *
     * @param title   Section heading text
     * @param cols    Column names for this table
     * @param isBooks true → Regular Book table;  false → Textbook table
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

        // ── Table (cells are not editable – changes go through the form) ──
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
        // Keep the ID column narrow – it is a reference, not main content
        tbl.getColumnModel().getColumn(COL_ID).setPreferredWidth(35);
        tbl.getColumnModel().getColumn(COL_ID).setMaxWidth(50);

        // ── Store table / model references so the form panel can reach them ──
        if (isBooks) { tblBooks = tbl; mdlBooks = mdl; }
        else         { tblTextbooks = tbl; mdlTextbooks = mdl; }

        // ── Selection listener ─────────────────────────────────────────────
        // When a row is selected:
        //   1. Clear the other table's selection (only one active row at a time)
        //   2. Enable Edit and Delete buttons on the form panel
        // When selection is cleared, disable both buttons again.
        tbl.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean hasSelection = tbl.getSelectedRow() >= 0;
            if (hasSelection) {
                if (isBooks  && tblTextbooks != null) tblTextbooks.clearSelection();
                if (!isBooks && tblBooks     != null) tblBooks.clearSelection();
            }
            if (btnEdit   != null) btnEdit  .setEnabled(hasSelection);
            if (btnDelete != null) btnDelete.setEnabled(hasSelection);
        });

        section.add(new JScrollPane(tbl), BorderLayout.CENTER);

        // ── Footer – record count only ──
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

    /** CREATE a new record, or UPDATE the existing one, depending on editingId. */
    private void onSave() {
        statusLabel.setText(" ");
        try {
            String  title    = txtTitle.getText().trim();
            String  pagesStr = txtPages.getText().trim();
            String  gradeStr = txtGradeLevel.getText().trim();
            boolean isText   = "Textbook".equals(cmbType.getSelectedItem());

            // Validation – collect all missing required fields before showing an error
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

    /** Load a record into the form for editing (triggered by the Edit button). */
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

    /** Show a confirmation dialog then DELETE the record (triggered by Delete button). */
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
     * Returns the ID of the currently selected row across BOTH tables.
     * Checks Regular Books first, then Textbooks.
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
        // Clearing table selection also triggers the listener which disables Edit/Delete
        tblBooks.clearSelection();
        tblTextbooks.clearSelection();
        txtTitle.requestFocus();
    }

    /** Enable / disable the Grade Level field depending on the selected type. */
    private void onTypeChange() {
        boolean isText = "Textbook".equals(cmbType.getSelectedItem());
        txtGradeLevel.setEnabled(isText);
        txtGradeLevel.setBackground(isText ? Color.WHITE : new Color(245, 245, 245));
        if (!isText) txtGradeLevel.setText("");
    }

    // ══════════════════════════════════════════
    //  TABLE REFRESH  (READ)
    //  Clears both tables and re-populates them
    //  from the database. Called on startup and
    //  after every Save / Update / Delete.
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
}
