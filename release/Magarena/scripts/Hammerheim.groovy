def List<MagicAbility> landwalkAbilities = [
    MagicAbility.Plainswalk,
    MagicAbility.Islandwalk,
    MagicAbility.Swampwalk,
    MagicAbility.Mountainwalk,
    MagicAbility.Forestwalk,
    MagicAbility.NonbasicLandwalk,
    MagicAbility.LegendaryLandwalk
];

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.LoseEvasion),
        "Lose landwalk"
    ) {

        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [new MagicTapEvent(source)];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source, final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                NEG_TARGET_CREATURE,
                this,
                "Target creature\$ loses all landwalk abilities until end of turn"
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new LoseAbilityAction(it,landwalkAbilities));
            });
        }
    }
]
