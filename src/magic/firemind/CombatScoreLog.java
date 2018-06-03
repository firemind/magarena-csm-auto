package magic.firemind;

import magic.model.ARG;
import magic.model.MagicAbility;
import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.choice.MagicCombatCreature;
import magic.model.choice.MagicDeclareAttackersResult;
import magic.model.choice.MagicDeclareBlockersResult;
import magic.utility.MagicFileSystem;
import magic.utility.MagicFileSystem.DataPath;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static void logAttacks(UUID choiceUuid, String ai, MagicGame startGame, double winPercentage, int numSim, int numParentSim, int lifePlayer, int lifeOpponent, int poisonPlayer, int poisonOpponent, MagicDeclareAttackersResult attackers, Object[] availableCreatures, Object[] blockers, int cardsInHand, int oppCardsInHand, long oppOpenMana) {


        String message = choiceUuid.toString()+";"+
                winPercentage+";"+
                numSim+";"+
                numParentSim+";"+
                lifePlayer+";"+
                lifeOpponent+";"+
                poisonPlayer+";"+
                poisonOpponent+";"+
                encodeAttacks(startGame, attackers, availableCreatures)+";" +
                encodeCreatures(availableCreatures)+";"+
                encodeCreatures(blockers)+";"+
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


    public static void logBlocks(UUID choiceUuid, String ai, MagicGame startGame, double winPercentage, int numSim, int numParentSim, int lifePlayer, int lifeOpponent, int poisonPlayer, int poisonOpponent, Object[] attackers, MagicDeclareBlockersResult blocks, Object[] availableBlockers, Object[] oppCreatures, int cardsInHand, int oppCardsInHand, long oppOpenMana) {

        String message = choiceUuid.toString()+";"+
                winPercentage+";"+
                numSim+";"+
                numParentSim+";"+
                lifePlayer+";"+
                lifeOpponent+";"+
                poisonPlayer+";"+
                poisonOpponent+";"+
                encodeCreatures(attackers)+";"+
                encodeBlocks(startGame, blocks, attackers, availableBlockers)+";"+
                encodeCreatures(availableBlockers)+";"+
                encodeCreatures(oppCreatures)+";"+
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

    private static String encodeAttacks(MagicGame startGame, MagicDeclareAttackersResult attackers, Object[] availableAttackers) {
        List<MagicPermanent> attackerList = new ArrayList<>();
        for(Object a: availableAttackers)
            attackerList.add((MagicPermanent) a);
        return "["+attackers.stream().mapToInt(e -> attackerList.indexOf(e.map(startGame))).mapToObj(Integer::toString).collect(Collectors.joining(","))+"]";
    }


    public static String encodeBlocks(MagicGame startGame, MagicDeclareBlockersResult blocks, Object[] attackers, Object[] blockers){
        List<MagicPermanent> attackerList = new ArrayList<>();
        for(Object a: attackers)
            attackerList.add((MagicPermanent) a);

        List<MagicPermanent> blockerList = new ArrayList<>();
        for(Object b: blockers)
            blockerList.add((MagicPermanent) b);

        return "["+blocks.stream().map(e -> encodeBlock(startGame, e, attackerList, blockerList)).collect(Collectors.joining(","))+"]";
    }

    public static String encodeBlock(MagicGame startGame, MagicCombatCreature[] block, List<MagicPermanent> attackers, List<MagicPermanent> blockers){
        return "{"+attackers.indexOf(block[0].permanent.map(startGame)) + "=" + Arrays.stream(block).skip(1).mapToInt(e -> blockers.indexOf(e.permanent.map(startGame))).mapToObj(Integer::toString).collect(Collectors.joining(","))+"}";

    }

    public static String encodeCreatures(Object[] oppCreatures){
        return "["+Arrays.stream(oppCreatures).map(e -> encodeCreature((MagicPermanent)e)).collect(Collectors.joining(","))+"]";
    }

    private static String encodeCreature(MagicPermanent oppCreature) {
        String res = oppCreature.getName();
        res = res+"["+oppCreature.getPower()+"/"+oppCreature.getLethalDamage(oppCreature.getToughness())+"]{";
        res += Stream.of(
                MagicAbility.Deathtouch,
                MagicAbility.DoubleStrike,
                MagicAbility.FirstStrike,
                MagicAbility.Flying,
                MagicAbility.Indestructible,
                MagicAbility.Lifelink,
                MagicAbility.Trample,
                MagicAbility.Vigilance,
                MagicAbility.Shadow,
                MagicAbility.Wither,
                MagicAbility.Exalted,
                MagicAbility.Infect,
                MagicAbility.BattleCry,
                MagicAbility.Afflict
                ).filter(oppCreature::hasAbility).map(a -> {
                    if(a == MagicAbility.Afflict){
                        int n = ARG.number(MagicAbility.Afflict.matched(oppCreature.getText()));
                        return "afflict "+n;
                    }else{
                       return a.getName();
                    }
                }).collect(Collectors.joining(","));
        res += "}";
        return res;
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
