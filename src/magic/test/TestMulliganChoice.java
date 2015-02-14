package magic.test;

import java.util.List;

import magic.ai.MagicAI;
import magic.ai.MagicAIImpl;
import magic.model.MagicDeckProfile;
import magic.model.MagicDuel;
import magic.model.MagicGame;
import magic.model.MagicPlayer;
import magic.model.MagicPlayerDefinition;
import magic.model.choice.MagicMulliganChoice;
import magic.model.phase.MagicMainPhase;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestMulliganChoice extends TestGameBuilder {
	public MagicGame getGame() {
		final MagicDuel duel = new MagicDuel();
		duel.setDifficulty(6);

		final MagicDeckProfile profile = new MagicDeckProfile("bgruw");
		final MagicPlayerDefinition player1 = new MagicPlayerDefinition(
				"Player", false, profile, 15);
		final MagicPlayerDefinition player2 = new MagicPlayerDefinition(
				"Computer", true, profile, 14);
		duel.setPlayers(new MagicPlayerDefinition[] { player1, player2 });
		duel.setStartPlayer(0);
		duel.setAIs(new MagicAI[] { null, MagicAIImpl.MCTS.getAI() });

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

	@Test
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
		List<Object[]> choices = mmc.getArtificialChoiceResults(game, null,
				player, null);
		assertTrue(choices.contains("no"));
	}
}