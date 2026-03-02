package minilibrary.database;

import minilibrary.model.TempBook;
import minilibrary.model.TempText;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────
//  BOOK DATABASE  (MySQL-backed CRUD)
//  Singleton – use BookDatabase.getInstance().
//  All four CRUD operations talk directly to
//  the books table in the mini_library DB.
// ─────────────────────────────────────────────
public class BookDatabase {

    // ── Singleton ─────────────────────────────
    private static BookDatabase instance;
    public  static BookDatabase getInstance() {
        if (instance == null) instance = new BookDatabase();
        return instance;
    }

    // ── CREATE ────────────────────────────────
    /** Inserts a new book record and syncs the generated ID back into the object. */
    public void add(TempBook book) {
        String sql = "INSERT INTO books (type, title, pages, grade_level) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getType());
            ps.setString(2, book.getTitle());
            ps.setInt   (3, book.getPages());
            // grade_level is NULL for Regular Books, filled for Textbooks
            if (book instanceof TempText tb) ps.setInt(4, tb.getGradeLevel());
            else                             ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();

            // Sync the MySQL auto-generated ID back into the Java object
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setDbId(keys.getInt(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── READ (all) ────────────────────────────
    /** Returns all book records ordered by ID. */
    public List<TempBook> getAll() {
        List<TempBook> list = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY id";
        try (Statement st   = DBConnection.getConnection().createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next()) {
                int    id    = rs.getInt("id");
                String type  = rs.getString("type");
                String title = rs.getString("title");
                int    pages = rs.getInt("pages");
                int    grade = rs.getInt("grade_level"); // returns 0 if NULL – safe for Regular Books

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
    /** Finds a single record by its ID. Returns Optional.empty() if not found. */
    public Optional<TempBook> findById(int id) {
        return getAll().stream().filter(b -> b.getId() == id).findFirst();
    }

    // ── UPDATE ────────────────────────────────
    /** Replaces the record that has the same ID as the updated object. */
    public boolean update(TempBook updated) {
        String sql = "UPDATE books SET type=?, title=?, pages=?, grade_level=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, updated.getType());
            ps.setString(2, updated.getTitle());
            ps.setInt   (3, updated.getPages());
            if (updated instanceof TempText tb) ps.setInt(4, tb.getGradeLevel());
            else                                ps.setNull(4, Types.INTEGER);
            ps.setInt(5, updated.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── DELETE ────────────────────────────────
    /** Removes the record with the given ID. */
    public boolean delete(int id) {
        String sql = "DELETE FROM books WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
