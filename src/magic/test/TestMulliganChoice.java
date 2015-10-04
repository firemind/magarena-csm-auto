package magic.test;

import java.util.List;


import magic.model.MagicDuel;
import magic.model.MagicGame;
import magic.model.MagicPlayer;
import magic.model.choice.MagicMulliganChoice;

public class TestMulliganChoice extends TestGameBuilder {
	public MagicGame getGame() {
		final MagicDuel duel = new MagicDuel();


		final MagicGame game = duel.nextGame();

		// game.setPhase(MagicMainPhase.getFirstInstance());
		//
		// MagicPlayer P = player;
		//
		// P.setLife(4);
		// createPermanent(game,P,"Rupture Spire",false,8);
		// createPermanent(game,P,"Hearthfire Hobgoblin", false, 4);
		// createPermanent(game,P,"Akrasan Squire", false, 4);
		//
		//
		// P = opponent;
		//
		// P.setLife(4);
		// createPermanent(game,P,"Rupture Spire",false,8);
		// createPermanent(game,P,"Hearthfire Hobgoblin", false, 4);
		// createPermanent(game,P,"Akrasan Squire", false, 4);

		return game;
	}

	public void test() {
		MagicGame game = getGame();
		MagicMulliganChoice mmc = new MagicMulliganChoice();
		final MagicPlayer player = game.getPlayer(0);
		final MagicPlayer opponent = game.getPlayer(1);

		addToLibrary(player, "Plains", 10);
		addToLibrary(opponent, "Plains", 10);
		// player.addCardToHand();
		addToHand(player, "Akrasan Squire", 4);
		addToHand(player, "Plains", 3);
		List<Object[]> choices = mmc.getArtificialChoiceResults(game, null);
		assert(choices.contains("no"));
	}
}