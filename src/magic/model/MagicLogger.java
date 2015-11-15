package magic.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import magic.data.GeneralConfig;
import magic.utility.MagicFileSystem;

public class MagicLogger {

    private final File logFile;
    private final StringBuilder sb = new StringBuilder();

    public MagicLogger(final String name, final String title) {
        logFile = MagicFileSystem.getDataPath(MagicFileSystem.DataPath.LOGS).resolve(name + ".log").toFile();
        initialize(title);
    }

    private void initialize(final String title) {
        sb.append(title);
        sb.append('\n');
        sb.append("CREATED ON ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        sb.append('\n');
        sb.append("MAGARENA VERSION ").append(GeneralConfig.VERSION);
        sb.append(", JRE ").append(System.getProperty("java.version"));
        sb.append(", OS ").append(System.getProperty("os.name"));
        sb.append("_").append(System.getProperty("os.version"));
        sb.append(" ").append(System.getProperty("os.arch"));
        sb.append("\n\n");
    }

    public void log(final String message) {
        sb.append(message).append("\n");
    }
    
    public void writeLog() {
        sb.append("WRITTEN ON ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        sb.append('\n');
        try (final PrintWriter writer = new PrintWriter(logFile)) {
            writer.println(sb.toString());
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to save " + logFile + " - " + ex);
        } finally {
            sb.setLength(0);
        }
    }

}
