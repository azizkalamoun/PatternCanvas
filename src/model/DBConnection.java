package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static Connection instance;
    private static boolean tablesInitialized = false;

    private static final String URL = "jdbc:mysql://localhost:3306/dessin?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private DBConnection() {}

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName(DRIVER);
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                instance.setAutoCommit(true);
                if (!tablesInitialized) {
                    try {
                        initTables();
                        tablesInitialized = true;
                    } catch (SQLException e) {
                        System.err.println("Failed to initialize tables: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver JDBC not found: " + e.getMessage());
            }
        }
        return instance;
    }

    private static void initTables() throws SQLException {
        String[] statements = {
            "CREATE TABLE IF NOT EXISTS shapes ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "type VARCHAR(50) NOT NULL, "
            + "x1 DOUBLE NOT NULL, y1 DOUBLE NOT NULL, "
            + "x2 DOUBLE NOT NULL, y2 DOUBLE NOT NULL, "
            + "color VARCHAR(50) DEFAULT 'RGB(0,0,0)', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS log ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "message TEXT NOT NULL, "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS drawings ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS drawing_shapes ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "drawing_id INT NOT NULL, "
            + "type VARCHAR(50) NOT NULL, "
            + "x1 DOUBLE NOT NULL, y1 DOUBLE NOT NULL, "
            + "x2 DOUBLE NOT NULL, y2 DOUBLE NOT NULL, "
            + "color VARCHAR(50) DEFAULT 'RGB(0,0,0)', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (drawing_id) REFERENCES drawings(id) ON DELETE CASCADE)"
        };
        try (Statement stmt = instance.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
            System.out.println("Database tables initialized successfully.");
        }
    }
    
    public static boolean testConnection() {
        try {
            Connection conn = getInstance();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection successful!");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static void closeConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                instance = null;
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}