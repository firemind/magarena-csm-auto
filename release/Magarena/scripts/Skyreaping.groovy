[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                this,
                "SN deals damage to each creature with flying equal to PN's devotion to green."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final int amount = event.getPlayer().getDevotion(MagicColor.Green);
            CREATURE_WITH_FLYING.filter(event) each {
                game.doAction(new DealDamageAction(event.getSource(),it,amount));
            }
        }
    }
]
