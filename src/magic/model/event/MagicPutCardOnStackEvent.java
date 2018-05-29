package magic.model.event;

import java.util.Collections;

import magic.model.MagicGame;
import magic.model.MagicCard;
import magic.model.MagicPlayer;
import magic.model.MagicLocationType;
import magic.model.MagicTuple;
import magic.model.stack.MagicCardOnStack;
import magic.model.action.PutItemOnStackAction;
import magic.model.action.RemoveCardAction;
import magic.model.action.MagicPermanentAction;

public class MagicPutCardOnStackEvent extends MagicEvent {
    public MagicPutCardOnStackEvent(final MagicCard source, final MagicPlayer player, final MagicLocationType fromLocation, final MagicLocationType toLocation, final MagicPermanentAction mod) {
        super(
            source,
            player,
            new MagicTuple(fromLocation, toLocation, mod),
            EVENT_ACTION,
            ""
        );
    }

    private static final MagicEventAction EVENT_ACTION = (final MagicGame game, final MagicEvent event) -> {
        final MagicTuple tup = event.getRefTuple();
        final MagicCardOnStack cardOnStack = new MagicCardOnStack(
            event.getCard(),
            event.getPlayer(),
            game.getPayedCost(),
            Collections.singletonList(tup.getMod(2))
        );
        final MagicLocationType from = tup.getLocationType(0);
        final MagicLocationType to = tup.getLocationType(1);
        cardOnStack.setFromLocation(from);
        cardOnStack.setMoveLocation(to);
        final MagicCard card = event.getCard();
        if (!card.isToken()) {
            game.doAction(new RemoveCardAction(card, from));
        }
        game.doAction(new PutItemOnStackAction(cardOnStack));
    };
}
