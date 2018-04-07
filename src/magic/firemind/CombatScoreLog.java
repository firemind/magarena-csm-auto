package magic.firemind;

import magic.model.MagicLogger;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.utility.MagicFileSystem;
import magic.utility.MagicFileSystem.DataPath;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CombatScoreLog {
    private CombatScoreLog() {}

    public static final String LOG_FILE = "combatScores.log";

    private static final String gameLog = (System.getProperty(LOG_FILE) != null) ?
        System.getProperty(LOG_FILE) :
        MagicFileSystem.getDataPath(DataPath.LOGS).resolve(LOG_FILE).toString();

    private static PrintWriter writer;

    public static String getLogFileName(){
        return gameLog;
    }

    public static void initialize() {
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(gameLog, true), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to create game log");
        }
    }

    public static void logAttacks(double winPercentage, int numSim, int lifePlayer, int lifeOpponent, MagicDeclareAttackersResult attackers, Object[] availableCreatures, Object[] blockers) {


        String message = winPercentage+";"+numSim+";"+lifePlayer+";"+lifeOpponent+";"+attackers.toString()+";"+ Arrays.toString(availableCreatures)+";"+ Arrays.toString(blockers);
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }

    }
    public static void log(final String message) {
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    public static void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
