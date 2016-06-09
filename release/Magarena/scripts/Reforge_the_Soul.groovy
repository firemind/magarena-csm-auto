[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                this,
                "Each player discards all the cards in his or her hand, then draws 7 cards."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            for (final MagicPlayer player : game.getAPNAP()) {
                game.addEvent(new MagicDiscardHandEvent(event.getSource(),player));
            }
            for (final MagicPlayer player : game.getAPNAP()) {
                game.addEvent(new MagicDrawEvent(event.getSource(), player, 7));
            }
        }
    }
]