package magic;

import magic.ai.FiremindAI;
import magic.ai.MagicAI;
import magic.ai.MagicAIImpl;
import magic.data.DeckUtils;
import magic.data.DuelConfig;
import magic.firemind.ScoringSet;
import magic.model.MagicDuel;
import magic.model.MagicGame;
import magic.model.MagicGameLog;
import magic.model.MagicGameReport;
import magic.model.MagicRandom;
import magic.ui.GameController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DeckStrCalGauntlet {

    private static int games = 10;
    private static int str1 = 1;
    private static int str2 = 1;
    private static int life = 20;
    private static int seed;
    private static String deck1 = "";
    private static String deck2 = "";
    private static MagicAIImpl ai1 = MagicAIImpl.FIREMIND;
    private static MagicAIImpl ai2 = MagicAIImpl.MMAB;

    // Command line parsing.
    private static boolean parseArguments(final String[] args) {
        boolean validArgs = true;
        for (int i = 0; i < args.length; i += 2) {
            final String curr = args[i];
            final String next = args[i+1];
            if ("--games".equals(curr)) {
                try { //parse CLI option
                    games = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! number of games not an integer");
                    validArgs = false;
                }
            } else if ("--str1".equals(curr)) {
                try { //parse CLI option
                    str1 = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! AI strength not an integer");
                    validArgs = false;
                }
            } else if ("--str2".equals(curr)) {
                try { //parse CLI option
                    str2 = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! AI strength not an integer");
                    validArgs = false;
                }
            } else if ("--deck1".equals(curr)) {
                deck1 = next;
            } else if ("--deck2".equals(curr)) {
                deck2 = next;
            } else if ("--ai1".equals(curr)) {
                try { //parse CLI option
                    ai1 = MagicAIImpl.valueOf(next);
                } catch (final IllegalArgumentException ex) {
                    System.err.println("Error: " + next + " is not valid AI");
                    validArgs = false;
                }
            } else if ("--ai2".equals(curr)) {
                try { //parse CLI option
                    ai2 = MagicAIImpl.valueOf(next);
                } catch (final IllegalArgumentException ex) {
                    System.err.println("Error: " + next + " is not valid AI");
                    validArgs = false;
                }
            } else if ("--life".equals(curr)) {
                try { //parse CLI option
                    life = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! starting life is not an integer");
                    validArgs = false;
                }
            } else if ("--seed".equals(curr)) {
                try { //parse CLI option
                    seed = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! seed is not an integer");
                    validArgs = false;
                }
            } else {
                System.err.println("Error: unknown option " + curr);
                validArgs = false;
            }
        }

        if (deck1.length() == 0) {
            System.err.println("Using player profile to generate deck 1");
        } else if (!(new File(deck1)).exists()) {
            System.err.println("Error: file " + deck1 + " does not exist");
            validArgs = false;
        }

        if (deck2.length() == 0) {
            System.err.println("Using player profile to generate deck 2");
        } else if (!(new File(deck2)).exists()) {
            System.err.println("Error: file " + deck2 + " does not exist");
            validArgs = false;
        }

        return validArgs;
    }

    private static MagicDuel setupDuel() {
        // Set the random seed
        if (seed != 0) {
            MagicRandom.setRNGState(seed);
            seed = MagicRandom.nextRNGInt(Integer.MAX_VALUE) + 1;
        }
        System.setProperty("rndSeed", ""+seed);

        // Set number of games.
        final DuelConfig config=new DuelConfig();
        config.setNrOfGames(games);
        config.setStartLife(life);

        // Set difficulty.
        final MagicDuel testDuel=new MagicDuel(config);
        testDuel.initialize();
        testDuel.setDifficulty(0, str1);
        testDuel.setDifficulty(1, str2);

        // Set the AI
        testDuel.setAIs(new MagicAI[]{ai1.getAI(), ai2.getAI()});
        testDuel.getPlayer(0).setArtificial(true);
        testDuel.getPlayer(1).setArtificial(true);

        // Set the deck.
        if (deck1.length() > 0) {
            DeckUtils.loadDeck(deck1, testDuel.getPlayer(0));
        }
        if (deck2.length() > 0) {
            DeckUtils.loadDeck(deck2, testDuel.getPlayer(1));
        }

        return testDuel;
    }

    public static void main(final String[] args) {
        // setup the handler for any uncaught exception
        final MagicGameReport reporter = new MagicGameReport();
        reporter.disableNotification();
        Thread.setDefaultUncaughtExceptionHandler(reporter);

        if (!parseArguments(args)) {
            System.err.println("Usage: java -cp <path to Magarena.jar/exe> magic.DeckStrCal --deck1 <.dec file> --deck2 <.dec file> [options]");
            System.err.println("Options:");
            System.err.println("--ai1      [MMAB|MMABC|MCTS|RND] (AI for player 1, default MMAB)");
            System.err.println("--ai2      [MMAB|MMABC|MCTS|RND] (AI for player 2, default MMAB)");
            System.err.println("--strength <1-8>                 (level of AI, default 6)");
            System.err.println("--games    <1-*>                 (number of games to play, default 10)");
            System.exit(1);
        }

        // Load cards and cubes.
        MagicMain.initializeEngine();
        MagicGameLog.initialize();

        for (final File fileEntry : (new File("Magarena/decks/prebuilt/")).listFiles()) {
            if (fileEntry.isFile()) {
	            runDuel(fileEntry.getAbsolutePath());
                System.out.println(fileEntry.getName());
            }
        }
        MagicGameLog.close();
    }

    private static void runDuel(String deckname) {
//    	FiremindAI fmai = (FiremindAI) ai1.getAI();
//    	fmai.scoringSet = new ScoringSet();
        final MagicDuel testDuel = setupDuel();

    	DeckUtils.loadDeck(deckname, testDuel.getPlayer(0));
    	DeckUtils.loadDeck(deckname, testDuel.getPlayer(1));
        System.out.println(
                 "#deck1" +
                "\tai1" +
                "\tstr1" +
                "\tdeck2" +
                "\tai2" +
                "\tstr2" +
                "\tgames" +
                "\td1win"+
                "\td1lose"
        );

        int played = 0;
        while (testDuel.getGamesPlayed() < testDuel.getGamesTotal()) {
            final MagicGame game=testDuel.nextGame();
            game.setArtificial(true);
            final GameController controller=new GameController(game);

            //maximum duration of a game is 60 minutes
            controller.setMaxTestGameDuration(3600000);

            controller.runGame();
            if (testDuel.getGamesPlayed() > played) {
                System.err.println(
                        deck1 + "\t" +
                        ai1 + "\t" +
                        str1 + "\t" +
                        deck2 + "\t" +
                        ai2 + "\t" +
                        str2 + "\t" +
                        testDuel.getGamesTotal() + "\t" +
                        testDuel.getGamesWon() + "\t" +
                        (testDuel.getGamesPlayed() - testDuel.getGamesWon())
                );
                played = testDuel.getGamesPlayed();
            }
        }
        System.out.println(
                deck1 + "\t" +
                ai1 + "\t" +
                str1 + "\t" +
                deck2 + "\t" +
                ai2 + "\t" +
                str2 + "\t" +
                testDuel.getGamesTotal() + "\t" +
                testDuel.getGamesWon() + "\t" +
                (testDuel.getGamesPlayed() - testDuel.getGamesWon())
        );
    }
}
