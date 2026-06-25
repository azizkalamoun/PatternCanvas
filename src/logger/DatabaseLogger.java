package logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import model.DBConnection;

public class DatabaseLogger implements LoggerStrategy {
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean tableChecked = false;

    @Override 
    public void log(String message) {
        try {
            Connection conn = DBConnection.getInstance();
            if (conn == null || conn.isClosed()) {
                System.out.println("[DBLogger] Database connection not available");
                return;
            }

            if (!tableChecked) {
                boolean created = ensureLogTableExists(conn);
                System.out.println("[DBLogger] Table check: created=" + created);
                tableChecked = true;

                try (Statement s = conn.createStatement()) {
                    var rs = s.executeQuery("SELECT DATABASE(), VERSION(), @@port");
                    if (rs.next()) {
                        System.out.println("[DBLogger] DB: " + rs.getString(1)
                            + ", Version: " + rs.getString(2)
                            + ", Port: " + rs.getInt(3));
                    }
                }
            }
            
            String timestamp = dtf.format(LocalDateTime.now());
            String fullMessage = "[" + timestamp + "] " + message;
            String sql = "INSERT INTO log (message) VALUES (?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, fullMessage);
                stmt.executeUpdate();
                try (var keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        System.out.println("[DBLogger] Inserted row ID " + keys.getInt(1) + ": " + fullMessage);
                    }
                }
            }

            try (Statement s = conn.createStatement()) {
                var rs = s.executeQuery("SELECT COUNT(*) FROM log");
                if (rs.next()) {
                    System.out.println("[DBLogger] Total rows in log table: " + rs.getInt(1));
                }
            }
        } catch (Exception e) {
            System.out.println("[DBLogger] ERROR: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    private boolean ensureLogTableExists(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS log ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "message TEXT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            return true;
        } catch (Exception e) {
            System.out.println("[DBLogger] Failed to create log table: " + e.getMessage());
            return false;
        }
    }
}