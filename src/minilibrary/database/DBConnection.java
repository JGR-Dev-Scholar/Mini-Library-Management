package minilibrary.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// ─────────────────────────────────────────────
//  DATABASE CONNECTION  (MySQL via JDBC)
//  Singleton connection – one shared instance
//  is reused across all database operations.
//  Change URL, USER, PASSWORD to match your
//  local MySQL setup before running.
// ─────────────────────────────────────────────
public class DBConnection {

    // ── Change these three values to match your MySQL setup ──
    private static final String URL      = "jdbc:mysql://localhost:3306/mini_library";
    private static final String USER     = "root";
    private static final String PASSWORD = " "; // ← your MySQL root password here

    private static Connection conn = null;

    /**
     * Returns a live connection, creating one if needed.
     * Throws SQLException so callers can distinguish a DB connection
     * failure from a wrong username/password in the users table.
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✔ MySQL connected.");
            }
        } catch (ClassNotFoundException e) {
            // Driver JAR is missing from lib/ – wrap and throw so the UI can catch it
            throw new SQLException("MySQL JDBC Driver not found. Check your lib/ folder.", e);
        }
        // DriverManager.getConnection already throws SQLException on bad credentials
        return conn;
    }

    /** Call this when the app closes to release the connection cleanly. */
    public static void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (Exception ignored) {}
    }
}
