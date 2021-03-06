[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                this,
                "PN returns two cards at random from his or her graveyard to his or her hand."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicPlayer player = event.getPlayer();
            for (final MagicCard card : player.getGraveyard().getRandomCards(2)) {
                game.doAction(new ShiftCardAction(card,MagicLocationType.Graveyard,MagicLocationType.OwnersHand));
            }
        }
    }
]
