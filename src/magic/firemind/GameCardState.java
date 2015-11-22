package magic.firemind;

import java.util.Collection;

import magic.model.MagicCard;
import magic.model.MagicCardDefinition;
import magic.model.MagicColor;
import magic.model.mstatic.MagicStatic;

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
    
    int getCardDefinitionScore(boolean inPlay) {
        if (card.isLand()) {
            int score=(int)(card.getValue()*50);
            for (final MagicColor color : MagicColor.values()) {
                score+=card.getManaSource(color)*50;
            }
            return score;
        }
        if(card.getName() == "Unknown"){
            return 350;
        }
        int score=(int)(card.getValue()*100);
        if(!inPlay){
           score -= card.getConvertedCost() * 20;
        }
        if (card.isCreature()) {
            return score+(card.getCardPower()+card.getCardToughness())*10;
        } else {
            return score;
        }
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
