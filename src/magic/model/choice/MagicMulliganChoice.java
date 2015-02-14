package magic.model.choice;

import magic.data.DuelConfig;
import magic.data.GeneralConfig;
import magic.model.MagicAbility;
import magic.model.MagicCard;
import magic.model.MagicCardDefinition;
import magic.model.MagicCostManaType;
import magic.model.MagicGame;
import magic.model.MagicManaCost;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;
import magic.model.MagicSource;
import magic.model.action.MagicPutIntoPlayAction;
import magic.model.event.MagicActivation;
import magic.model.event.MagicEvent;
import magic.model.event.MagicManaActivation;
import magic.model.event.MagicSourceActivation;
import magic.model.phase.MagicMainPhase;
import magic.ui.GameController;
import magic.ui.UndoClickedException;
import magic.ui.duel.choice.MayChoicePanel;
import magic.ui.duel.choice.MulliganChoicePanel;
import magic.ui.screen.MulliganScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class MagicMulliganChoice extends MagicChoice {

    private static final List<Object[]> YES_CHOICE_LIST =
            Collections.singletonList(new Object[]{YES_CHOICE});
    private static final List<Object[]> NO_CHOICE_LIST =
            Collections.singletonList(new Object[]{NO_CHOICE});
    private static final List<Object[]> ACTUAL_CHOICE_LIST =
            Collections.singletonList(new Object[]{NO_CHOICE,YES_CHOICE});

    public MagicMulliganChoice() {
        super("");
    }

    @Override
    Collection<Object> getArtificialOptions(
            final MagicGame game,
            final MagicEvent event,
            final MagicPlayer player,
            final MagicSource source) {
        throw new UnsupportedOperationException();
    }
    

    @Override
    public List<Object[]> getArtificialChoiceResults(
            final MagicGame game,
            final MagicEvent event,
            final MagicPlayer player,
            final MagicSource source) {

    	int costSum = 0;
    	List<MagicCard> deck = new ArrayList<>();
    	deck.addAll(player.getLibrary());
    	deck.addAll(player.getHand());
    	for(MagicCard card: deck){
    		costSum += card.getConvertedCost();
    	}
		int minLands = 2;
		int maxLands = 3;
    	if(costSum > 90){
    		minLands = 3;
    		maxLands = 4;
    	}else if(costSum > 70){
    		minLands = 2;
    		maxLands = 4;
    	}
        if(game.getPlayer(0) != player){
            return getArtificialChoiceResultsOld(game,event,player,source);
        }
        if (player.getHandSize() <= 5) {
            System.out.print("+ ");
            printHand(player.getHand());
            return NO_CHOICE_LIST;
        }

		final MagicGame assumedGame = new MagicGame(game, player);
		MagicPlayer assumedPlayer = assumedGame.getPlayer(player.getIndex());
		assumedGame.setPhase(MagicMainPhase.getFirstInstance());
		long idcount = 0;
		List<MagicCard> lands = new ArrayList<>();
        for (final MagicCard card : assumedPlayer.getHand()) {
            final MagicCardDefinition cardDefinition = card.getCardDefinition();
            if (cardDefinition.isLand()) {
            	final MagicPermanent permanent = new MagicPermanent(idcount++,card, assumedPlayer);
            	assumedGame.doAction(new MagicPutIntoPlayAction() {
                    @Override
                    protected MagicPermanent createPermanent(final MagicGame game) {
                        return permanent;
                    }
                });
                lands.add(card);
            }
        }

        int playableCards = 0;
        for (final MagicCard card : assumedPlayer.getHand()) {
            final MagicCardDefinition cardDefinition = card.getCardDefinition();
            if (!cardDefinition.isLand() ) {
            	if(playableWith(card, assumedGame, assumedPlayer)){
            		playableCards++;
//            		System.out.println("Can play "+card.getName());
//            	}else{
//            		System.out.println("Can not play "+card.getName());
            	}
            }
        }
        
        if(player.getHandSize() > 6){
	        if(playableCards > 1){
	            System.out.print("+ ");
	            printHand(player.getHand());
	            return NO_CHOICE_LIST;
	        }
        }else{
	        if(playableCards > 0){
	            System.out.print("+ ");
	            printHand(player.getHand());
	            return NO_CHOICE_LIST;
	        }
        }
//        
//        if (lands >= maxLands) {
//            return NO_CHOICE_LIST;
//        }
        if(lands.size() < minLands || lands.size() > maxLands){
            System.out.print("- ");
            printHand(player.getHand());
            return YES_CHOICE_LIST;
        }else{
        	System.out.print("~ ");
	        printHand(player.getHand());
	        return ACTUAL_CHOICE_LIST;
        }
    }
    
    private void printHand(List<MagicCard> hand){
        for(MagicCard card: hand){
        	System.out.print(card.getName()+", ");
        }
        System.out.println("");
    }
    
    private boolean playableWith(MagicCard toPlay, MagicGame game, MagicPlayer player){
    	MagicManaCost mmc = toPlay.getCardDefinition().getCost();
    	List<MagicCostManaType> mcmts =  mmc.getCostManaTypes(0);
//    	for(MagicCard land: lands){
//    		land.getCardDefinition().getManaActivations()
//    	}
        final MagicBuilderManaCost builderCost=new MagicBuilderManaCost();
        builderCost.addTypes(mcmts);
        final MagicPayManaCostResultBuilder builder=new MagicPayManaCostResultBuilder(game,player,builderCost);
        return builder.hasResults();
    }
//
    
    public List<Object[]> getArtificialChoiceResultsOld(
            final MagicGame game,
            final MagicEvent event,
            final MagicPlayer player,
            final MagicSource source) {
        int minLands = (game.getTurnPlayer() == player) ? 3 : 2;
        final int maxLands = 4;
        int lands = 0;
        int high = 0;
        int nonLandManaSources = 0;

        if (player.getHandSize() <= 5) {
            return NO_CHOICE_LIST;
        }

        for (final MagicCard card : player.getHand()) {
            final MagicCardDefinition cardDefinition = card.getCardDefinition();
            if (cardDefinition.isLand()) {
                lands++;
            } else {
                if (!cardDefinition.getManaActivations().isEmpty()) {
                    nonLandManaSources++;
                }
                if (cardDefinition.getConvertedCost()>4) {
                    high++;
                }
            }
        }

        if (nonLandManaSources >= 1) {
            minLands--;
        }
        if (lands >= minLands && lands <= maxLands && high <= 2) {
            return NO_CHOICE_LIST;
        }

        return YES_CHOICE_LIST;
    }
//    
//    private MagicGame useFetches(MagicGame game){
//    	final MagicEvent event = game.getNextEvent();
//    	List<Object[]> choices = event.getArtificialChoiceResults(game);
//    	int manaAvailable = 0;
//    	int differentManaAvailable = 0;
//
//        final List<ArtificialChoiceResults> achoices=new ArrayList<ArtificialChoiceResults>();
//        for (final Object[] choice : choices) {
//            final ArtificialChoiceResults achoice=new ArtificialChoiceResults(choice);
//            achoices.add(achoice);
//            
//            final MagicGame workerGame=new MagicGame(sourceGame,scorePlayer);
//    }

    @Override
    public Object[] getPlayerChoiceResults(
            final GameController controller,
            final MagicGame game,
            final MagicPlayer player,
            final MagicSource source) throws UndoClickedException {

        if (player.getHandSize() <= 1) {
            return new Object[]{NO_CHOICE};
        }
        controller.disableActionButton(false);
        final MayChoicePanel choicePanel = controller.waitForInput(new Callable<MayChoicePanel>() {
            public MayChoicePanel call() {
                final boolean showMulliganScreen =
                        MulliganScreen.isActive() ||
                        (player.getHandSize() == DuelConfig.getInstance().getHandSize() && GeneralConfig.getInstance().showMulliganScreen());
                if (showMulliganScreen) {
                    return new MulliganChoicePanel(controller, source, "You may take a mulligan.", player.getPrivateHand());
                } else {
                    return new MayChoicePanel(controller,source,"You may take a mulligan.");
                }
            }
        });
        if (choicePanel.isYesClicked()) {
            return new Object[]{YES_CHOICE};
        }
        return new Object[]{NO_CHOICE};
    }
}
