[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                NEG_TARGET_CREATURE_WITH_FLYING,
                new MagicLoseAbilityTargetPicker(MagicAbility.Flying),
                this,
                "SN deals 2 to target creature with flying. That creature loses flying until end of turn."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new DealDamageAction(event.getSource(),it,2));
                game.doAction(new LoseAbilityAction(it,MagicAbility.Flying));
            });
        }
    }
]
