def choice = Positive("target Chimera creature");

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Pump),
        "+Counter"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                new MagicSacrificeEvent(source)
            ];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source, final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                choice,
                this,
                "Put a +2/+2 counter on target Chimera creature\$. It gains flying."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new ChangeCountersAction(it,MagicCounterType.PlusTwo,1));
                game.doAction(new GainAbilityAction(it, MagicAbility.Flying, MagicStatic.Forever));
            });
        }
    }
]
