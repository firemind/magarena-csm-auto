package magic.firemind;

import magic.model.MagicCard;
import magic.model.MagicCardDefinition;

public class GameCardState {

    private final MagicCardDefinition card;
    private final int quantity;
    private final boolean isTapped;

    GameCardState(MagicCardDefinition card, int quantity, boolean tapped) {
        this.card = card;
        this.quantity = quantity;
        this.isTapped = tapped;                
    }
    GameCardState(MagicCardDefinition card, int quantity) {
        this(card, quantity, false);
    }

    public String getCardName() {
        return card.getName();
    }

    public MagicCardDefinition getCardDef() {
        return card;
    }
    
    public int getQuantity() {
        return quantity;
    }

    public boolean isTapped() {
        return isTapped;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof GameCardState)) {
            return false;
        } else {
            final GameCardState other = (GameCardState)obj;
            return other.card.getName().equals(getCardName()) &&
                   other.isTapped() == isTapped();
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (card.getName() == null ? 0 : card.getName().hashCode());
        hash = 37 * hash + (isTapped ? 1 : 0);
        return hash;
    }

}
