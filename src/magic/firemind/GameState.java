package magic.firemind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import magic.firemind.GameCardState;
import magic.model.MagicCard;
import magic.model.MagicCardDefinition;
import magic.model.MagicGame;
import magic.model.MagicPermanent;
import magic.model.MagicPlayer;

public class GameState {
    private final List<GameCardState> library = new ArrayList<>();
    private final List<GameCardState> hand = new ArrayList<>();
    private final List<GameCardState> permanents = new ArrayList<>();
    private final List<GameCardState> graveyard = new ArrayList<>();
    private final List<GameCardState> exiled = new ArrayList<>();
    private int life;
    private int poison;

    private ScoringSet scoringSet;
    
	
	public GameState(MagicPlayer player, ScoringSet scoringSet){
        //this.numberOfPermanents = (short) game.getPlayer(0).getNrOfPermanents();
        //final MagicPlayer player = game.getScorePlayer();
        this.life = player.getLife();
        this.poison = player.getPoison();
        this.savePlayerLibraryState(player);
        this.savePlayerHandState(player);
        savePlayerPermanentsState(player);
        savePlayerGraveyardState(player);
        savePlayerExiledState(player);
        this.scoringSet = scoringSet;
	}
	
    private void savePlayerPermanentsState(final MagicPlayer player) {
        for (final MagicPermanent card : player.getPermanents()) {
            final GameCardState tsCard = new GameCardState(card.getCard().getCardDefinition(), 1, card.isTapped());
        	permanents.add(tsCard);
        }
    }

    private void savePlayerGraveyardState(final MagicPlayer player) {
    	for (MagicCard card : player.getGraveyard()) {
        	graveyard.add(new GameCardState(card.getCardDefinition(), 1));
        }
    }

    private void savePlayerHandState(final MagicPlayer player) {
    	for (MagicCard card :player.getHand()) {
    		hand.add(new GameCardState(card.getCardDefinition(), 1));
        }
    }

    private void savePlayerLibraryState(final MagicPlayer player) {
    	for (MagicCard card :player.getLibrary()) {
    		library.add(new GameCardState(card.getCardDefinition(), 1));
        }
    }

    private void savePlayerExiledState(final MagicPlayer player) {
    	for (MagicCard card :player.getExile()) {
    		exiled.add(new GameCardState(card.getCardDefinition(), 1));
        }
    }
    
    public int getScore() {
		// int totalPower = permanents.stream().filter(gcs -> gcs.getCardDef().isCreature()).mapToInt(gcs->gcs.getCardDef().getCardPower()).sum();
		//int totalToughness = permanents.stream().filter(gcs -> gcs.getCardDef().isCreature()).mapToInt(gcs->gcs.getCardDef().getCardToughness()).sum();

		int totalPower = 0;
		int totalToughness = 0;
		int permanentValues = 0;
		MagicCardDefinition mcd;
		for(GameCardState gcs : permanents){
			permanentValues += gcs.isTapped() ?  scoringSet.get("scorePermanents") : scoringSet.get("scorePermanents")+1;
			if((mcd = gcs.getCardDef()).isCreature()){
			  totalPower += mcd.getCardPower();	
			  totalToughness += mcd.getCardToughness();
			}
		}
		//return (int) (Math.random()*1000);
		// TODO go wider, consider multiple outcomes for same action
		//    -> this leads to the question: is draw different or same in all simulations?
		//    -> alternatively, calculate probability of drawing each remaining card (taking into account nr of copies remaining)
		//    -> Note: currently library state is fixed so the drawn card is always the same
		// TODO add learning element
		// TODO calculate scoring with gen algo
		int score =  permanentValues + 
				totalPower * scoringSet.get("scorePower") +
				totalToughness * scoringSet.get("scoreToughness") + 
				hand.size() * scoringSet.get("scoreHand") +
				graveyard.size() * scoringSet.get("scoreGraveyard") +
				exiled.size() * scoringSet.get("scoreExiled") + 
				life * scoringSet.get("scoreLife") + 
				poison * scoringSet.get("scorePoison");
//		System.out.println("Scored state (Life:"+life+"): "+score);
//		System.out.print("Hand: "); printCards(hand);
//		System.out.print("Board: "); printCards(permanents);
		return score;
	}
    
    private void printCards(List<GameCardState> cards){
        for(GameCardState card: cards){
        	System.out.print(card.getCardName()+", ");
        }
        System.out.println("");
    }

}
