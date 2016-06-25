def TEXT1 = "Search your library for up to two creature cards, reveal them, put them into your hand, then shuffle your library."

def TEXT2 = "Put up to two creature cards from your hand onto the battlefield."

def choice = new MagicTargetChoice("a creature card from your hand");

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                payedCost.isKicked() ? 
                    MagicChoice.NONE :
                    new MagicOrChoice(
                        MagicChoice.NONE,
                        MagicChoice.NONE
                    ),
                this,
                payedCost.isKicked() ?
                    "Search your library for up to two creature cards, reveal them, put them into your hand, then shuffle your library."+
                    " Put up to two creature cards from your hand onto the battlefield." :
                    "Choose one\$ — • " + TEXT1 + " • " + TEXT2 + "\$"
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isKicked()) {
            final List<MagicCard> choiceList = event.getPlayer().filterCards(MagicTargetFilterFactory.CREATURE_CARD_FROM_LIBRARY);
                game.addEvent(new MagicSearchToLocationEvent(
                    event,
                    new MagicFromCardListChoice(choiceList, 2, true),
                    MagicLocationType.OwnersHand
                ));
                game.addEvent(new MagicPutOntoBattlefieldEvent(
                    event,
                    new MagicMayChoice(choice)
                ));  
                game.addEvent(new MagicPutOntoBattlefieldEvent(
                    event,
                    new MagicMayChoice(choice)
                ));
            } else if (event.isMode(1)) {
            final List<MagicCard> choiceList = event.getPlayer().filterCards(MagicTargetFilterFactory.CREATURE_CARD_FROM_LIBRARY);
                game.addEvent(new MagicSearchToLocationEvent(
                    event,
                    new MagicFromCardListChoice(choiceList, 2, true),
                    MagicLocationType.OwnersHand
                ));
            } else if (event.isMode(2)) {
                game.addEvent(new MagicPutOntoBattlefieldEvent(
                    event,
                    new MagicMayChoice(choice)
                ));
                game.addEvent(new MagicPutOntoBattlefieldEvent(
                    event,
                    new MagicMayChoice(choice)
                ));
            }
        }
    }
]
