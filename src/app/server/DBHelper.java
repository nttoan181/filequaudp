package app.server;

import java.sql.*;

public class DBHelper {
    private static final String DB_URL = "jdbc:sqlite:data/storage.db";

    public static void init() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "filename TEXT NOT NULL," +
                "stored_path TEXT NOT NULL," +
                "filesize INTEGER," +
                "total_chunks INTEGER," +
                "uploader TEXT," +
                "uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
