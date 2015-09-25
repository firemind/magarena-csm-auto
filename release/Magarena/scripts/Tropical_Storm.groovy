[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            final int amount = payedCost.getX();
            return new MagicEvent(
                cardOnStack,
                this,
                "SN deals "+amount+" damage to each creature with flying and 1 additional damage to each blue creature."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicSource source = event.getSource();
            final int amount = event.getCardOnStack().getX();
            CREATURE_WITH_FLYING.filter(event) each {
                game.doAction(new DealDamageAction(source, it, amount));
            }
            BLUE_CREATURE.filter(event) each {
                game.doAction(new DealDamageAction(source, it, 1));
            }
        }
    }
]
