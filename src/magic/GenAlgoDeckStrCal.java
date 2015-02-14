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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.collections.transformation.SortedList;

public class GenAlgoDeckStrCal {

    private static int games = 10;
    private static int repeat = 1;
    private static int str1 = 2;
    private static int str2 = 2;
    private static int life = 20;
    private static int seed;
    private static String deck1 = "";
    private static String deck2 = "";
    private static MagicAIImpl ai1 = MagicAIImpl.FIREMIND;
    private static MagicAIImpl ai2 = MagicAIImpl.MMAB;

    private static ScoringSet currentScoringSet = new ScoringSet(); 
    private static SortedSet<ScoringSet> allScoringSets = new TreeSet(new Comparator<ScoringSet>(){
        public int compare(ScoringSet p1, ScoringSet p2) {
            return p2.fitness- p1.fitness;
        }
    }); 
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
            } else if ("--repeat".equals(curr)) {
                try { //parse CLI option
                    repeat = Integer.parseInt(next);
                } catch (final NumberFormatException ex) {
                    System.err.println("ERROR! repeat is not an integer");
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

    private static ScoringSet crossover(ScoringSet setA, ScoringSet setB) {
		ScoringSet newScoringSet = new ScoringSet();
    	for(String key: newScoringSet.keys()){
    		newScoringSet.put(key, Math.random() >= 0.5 ? setA.get(key) : setB.get(key));
    	}
		return newScoringSet;
	}
    
    private static ScoringSet mutate(ScoringSet origScoringSet) {
		ScoringSet newScoringSet = new ScoringSet(origScoringSet);
    	for(String key: newScoringSet.mutatableKeys()){
    		if(Math.random() < 0.2){
        		newScoringSet.put(key, newScoringSet.get(key) + (Math.random() >= 0.5 ? 1 : -1));
    		}
    	}
		return newScoringSet;
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
        
        currentScoringSet.fitness = games/2;
		allScoringSets.add(currentScoringSet);
        for (int i = 0; i < repeat; i++) {
        	for(int j=0;j<8;j++){
        		currentScoringSet = mutate(allScoringSets.first());
        		if(!allScoringSets.contains(currentScoringSet)){
        			runDuel();
        		}
        	}
        	for(int j=0;j<8;j++){
        		if(!allScoringSets.contains(currentScoringSet)){
        			currentScoringSet = crossover(pickBreedingCandidate(), pickBreedingCandidate());
        		}
                runDuel();
        	}
        }
        currentScoringSet.print();
        MagicGameLog.close();
    }

	private static ScoringSet pickBreedingCandidate(){
		while(true){
			for(ScoringSet set : allScoringSets){
				if (Math.random() >= 0.5){
					return set;
				}
			}
		}
	}
    private static void runDuel() {
        //currentScoringSet.print();
        FiremindAI fmai = (FiremindAI) ai1.getAI();
        fmai.scoringSet = currentScoringSet;
        final MagicDuel testDuel = setupDuel();

        System.out.println(
                 "#deck1" +
                "\tai1" +
                "\t\tstr1" +
                "\tdeck2" +
                "\tai2" +
                "\tstr2" +
                "\tgames" +
                "\td1win"+
                "\td1lose"
        );

        int played = 0;
        while (testDuel.getGamesPlayed() < testDuel.getGamesTotal()) {
        	if(testDuel.getGamesPlayed() == 0)
        		loadDecks("burn", testDuel);
        	if(testDuel.getGamesPlayed() == 10)
        		loadDecks("merfolk", testDuel);
        	if(testDuel.getGamesPlayed() == 20)
        		loadDecks("affinity", testDuel);
        	if(testDuel.getGamesPlayed() == 30)
        		loadDecks("domain", testDuel);
        	if(testDuel.getGamesPlayed() == 40)
        		loadDecks("soul_sisters", testDuel);
        	
            final MagicGame game=testDuel.nextGame();
            game.setArtificial(true);
            final GameController controller=new GameController(game);

            //maximum duration of a game is 60 minutes
            controller.setMaxTestGameDuration(3600000);

            controller.runGame();
            if (testDuel.getGamesPlayed() > played) {
            	printDuelState(testDuel);

                played = testDuel.getGamesPlayed();
            }
        }

    	printDuelState(testDuel);

        currentScoringSet.fitness = testDuel.getGamesWon();
		allScoringSets.add(currentScoringSet);
        currentScoringSet.print();
    }

	private static void printDuelState(MagicDuel testDuel) {
        System.err.println(
                deck1 + "\t" +
                ai1 + "\t\t" +
                str1 + "\t" +
                deck2 + "\t" +
                ai2 + "\t" +
                str2 + "\t" +
                testDuel.getGamesTotal() + "\t" +
                testDuel.getGamesWon() + "\t" +
                (testDuel.getGamesPlayed() - testDuel.getGamesWon())
        );
		
	}
	private static void loadDecks(String deck, MagicDuel testDuel){
        DeckUtils.loadDeck(deck+".dec", testDuel.getPlayer(0));
        DeckUtils.loadDeck(deck+".dec", testDuel.getPlayer(1));
	}
}
