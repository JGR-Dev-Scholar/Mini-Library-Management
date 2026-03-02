package minilibrary.model;

import java.util.concurrent.atomic.AtomicInteger;

// ─────────────────────────────────────────────
//  MODEL: TempBook  (Regular Book)
//  Base entity stored in the database.
//  TempText extends this class for Textbooks.
// ─────────────────────────────────────────────
public class TempBook {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);
    private final int id;
    private String title;
    private int    pages;

    /** Normal constructor – assigns a new auto-increment ID. */
    public TempBook()               { this.id = ID_GEN.getAndIncrement(); }
    /** Edit constructor – reuses an existing ID (used when loading from MySQL). */
    public TempBook(int existingId) { this.id = existingId; }

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

    // ── Getters ───────────────────────────────
    public int    getId()    { return id; }
    public String getTitle() { return title; }
    public int    getPages() { return pages; }
    public String getType()  { return "Regular Book"; }

    // ── Setters (with validation) ─────────────
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
