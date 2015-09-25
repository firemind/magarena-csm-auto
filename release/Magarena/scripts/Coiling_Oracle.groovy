[
    new MagicWhenComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game, final MagicPermanent permanent, final MagicPayedCost payedCost) {
            return new MagicEvent(
                permanent,
                this,
                "Reveal the top card of PN's library. If that card is a land card, put it onto the battlefield. " + 
                " Otherwise, put that card into your hand."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            for (final MagicCard card : event.getPlayer().getLibrary().getCardsFromTop(1)) {
                game.doAction(new RevealAction(card));
                if (card.hasType(MagicType.Land)) {
                    game.doAction(new ReturnCardAction(
                        MagicLocationType.OwnersLibrary,
                        card,
                        event.getPlayer()
                    ));
                } else {
                    game.doAction(new ShiftCardAction(
                        card,
                        MagicLocationType.OwnersLibrary,
                        MagicLocationType.OwnersHand
                    ));
                }
            }
        }
    }
]
