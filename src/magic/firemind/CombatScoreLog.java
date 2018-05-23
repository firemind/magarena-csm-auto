package magic.firemind;

import magic.model.MagicLogger;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.choice.MagicDeclareBlockersChoice;
import magic.model.choice.MagicDeclareBlockersResult;
import magic.utility.MagicFileSystem;
import magic.utility.MagicFileSystem.DataPath;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CombatScoreLog {
    private CombatScoreLog() {}

    public static final String ATTACK_LOG_FILE = "combatAttackScores.log";
    public static final String BLOCK_LOG_FILE = "combatBlockScores.log";
    private static String duelConfig;

    private static final String attackLog = (System.getProperty(ATTACK_LOG_FILE) != null) ?
        System.getProperty(ATTACK_LOG_FILE) :
        MagicFileSystem.getDataPath(DataPath.LOGS).resolve(ATTACK_LOG_FILE).toString();

    private static final String blockLog = (System.getProperty(BLOCK_LOG_FILE) != null) ?
            System.getProperty(BLOCK_LOG_FILE) :
            MagicFileSystem.getDataPath(DataPath.LOGS).resolve(BLOCK_LOG_FILE).toString();

    private static PrintWriter attackWriter;
    private static PrintWriter blockWriter;

    public static void initialize(String duelConfig) {
        CombatScoreLog.duelConfig = duelConfig;
        try {
            attackWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(attackLog, true), StandardCharsets.UTF_8));
            blockWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(blockLog, true), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to create game log");
        }
    }

    public static void logAttacks(String ai, double winPercentage, int numSim, int numParentSim, int lifePlayer, int lifeOpponent, MagicDeclareAttackersResult attackers, Object[] availableCreatures, Object[] blockers, int cardsInHand, int oppCardsInHand, long oppOpenMana) {


        String message = winPercentage+";"+
                numSim+";"+
                numParentSim+";"+
                lifePlayer+";"+
                lifeOpponent+";"+
                attackers.toString()+";"
                + Arrays.toString(availableCreatures)+";"+
                Arrays.toString(blockers)+";"+
                cardsInHand+";"+
                oppCardsInHand+";"+
                oppOpenMana+";"+
                ai +";"+
                duelConfig;
        if (attackWriter!= null) {
            attackWriter.println(message);
            attackWriter.flush();
        }

    }

    public static void logBlocks(String ai, double winPercentage, int numSim, int numParentSim, int lifePlayer, int lifeOpponent, Object[] attackers, MagicDeclareBlockersResult[] blocks, Object[] availableBlockers, Object[] oppCreatures, int cardsInHand, int oppCardsInHand, long oppOpenMana) {

        String message = winPercentage+";"+
                numSim+";"+
                numParentSim+";"+
                lifePlayer+";"+
                lifeOpponent+";"+
                Arrays.toString(attackers)+";"+
                Arrays.toString(blocks)+";"+
                Arrays.toString(availableBlockers)+";"+
                Arrays.toString(oppCreatures)+";"+
                cardsInHand+";"+
                oppCardsInHand+";"+
                oppOpenMana+";"+
                ai+";"+
                duelConfig;
        if (blockWriter!= null) {
            blockWriter.println(message);
            blockWriter.flush();
        }

    }
    public static void close() {
        if (attackWriter != null) {
            attackWriter.close();
        }
        if (blockWriter != null) {
            blockWriter.close();
        }
    }
}
