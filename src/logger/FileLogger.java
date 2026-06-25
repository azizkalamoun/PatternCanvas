package logger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger implements LoggerStrategy {
    private final String filename;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileLogger() {
        String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        this.filename = "log_" + timeStamp + ".txt";
    }

    @Override
    public void log(String message) {
        String timestamp = dtf.format(LocalDateTime.now());
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write("[" + timestamp + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Erreur fichier log : " + e.getMessage());
        }
    }
}
