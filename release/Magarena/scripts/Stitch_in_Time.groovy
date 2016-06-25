[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicCoinFlipChoice(),
                this,
                "PN flips a coin.\$ If PN wins the flip, PN takes an extra turn after this one."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicPlayer player = event.getPlayer();
            final Boolean heads = event.isMode(1) 
            game.addEvent(new MagicCoinFlipEvent(
                event.getSource(),
                heads,
                player,
                new ChangeExtraTurnsAction(player,1),
                null
            ));
        }
    }
]
