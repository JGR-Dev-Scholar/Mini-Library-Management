package minilibrary.model;

// ─────────────────────────────────────────────
//  MODEL: TempText  (Textbook)
//  Extends TempBook and adds a grade level.
//  grade_level is stored as NULL in MySQL for
//  Regular Books and filled in for Textbooks.
// ─────────────────────────────────────────────
public class TempText extends TempBook {
    private int gradeLevel;

    /** Normal constructor – assigns a new auto-increment ID. */
    public TempText()               { super(); }
    /** Edit constructor – reuses an existing ID (used when loading from MySQL). */
    public TempText(int existingId) { super(existingId); }

    // ── Getter ────────────────────────────────
    public int getGradeLevel() { return gradeLevel; }

    // ── Setter (with validation) ──────────────
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
