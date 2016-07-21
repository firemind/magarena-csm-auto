package magic.ui.screen;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import magic.model.MagicCardDefinition;
import magic.model.MagicDeck;
import magic.ui.cardtable.CardTablePanel;
import magic.ui.deck.editor.DeckSideBar;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class DeckViewPanel extends JPanel {

    private MagicDeck deck;
    private final MigLayout migLayout = new MigLayout();
    private final DeckSideBar sideBarPanel;
    private final CardTablePanel deckTable;

    public DeckViewPanel(final MagicDeck aDeck, final MagicCardDefinition selectedCard) {

        this.deck = aDeck;

        sideBarPanel = new DeckSideBar();
        sideBarPanel.setDeck(deck);

        deckTable = new CardTablePanel(this.deck, "  " + this.deck.getName());
        deckTable.setDeckEditorSelectionMode();
        deckTable.setHeaderVisible(false);
        deckTable.showCardCount(true);
        setDeckTablePropChangeListeners();

        setLookAndFeel();
        refreshLayout();

        if (selectedCard != null) {
            deckTable.setSelectedCard(selectedCard);
        } else {
            deckTable.setSelectedCard(null);
        }

    }

    public DeckViewPanel(final MagicDeck aDeck) {
        this(aDeck, null);
    }

    private void setDeckTablePropChangeListeners() {
        deckTable.addPropertyChangeListener(
                CardTablePanel.CP_CARD_SELECTED,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        setCard(getSelectedCard());
                    }
                });
    }

    private MagicCardDefinition getSelectedCard() {
        if (deckTable.getSelectedCards().size() > 0) {
            return deckTable.getSelectedCards().get(0);
        } else {
            return MagicCardDefinition.UNKNOWN;
        }
    }

    private void setCard(final MagicCardDefinition card) {
        final int cardCount = deck.getCardCount(card);
        sideBarPanel.setCard(card);
        sideBarPanel.setCardCount(cardCount);
    }

    private void setLookAndFeel() {
        setOpaque(false);
        setLayout(migLayout);
    }

    private void refreshLayout() {
        removeAll();
        migLayout.setLayoutConstraints("insets 0, gap 0");
        add(sideBarPanel, "h 100%");
        add(deckTable, "w 100%, h 100%");
    }

    public MagicDeck getDeck() {
        return this.deck;
    }

    void setDeck(MagicDeck aDeck) {
        this.deck = aDeck;
    }

}
