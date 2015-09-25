def choice = Negative("target artifact, creature, or land");

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Tapping),
        "Tap"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                new MagicTapEvent(source), new MagicPayManaCostEvent(source, "{4}")
            ];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source, final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                choice,
                MagicTapTargetPicker.Tap,
                this,
                "Tap target artifact, creature or land\$. It doesn't untap during its controller's untap step for as long as SN remains tapped."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.doAction(new TapAction(it));
                game.doAction(new AddStaticAction(
                    event.getPermanent(),
                    MagicStatic.AsLongAsCond(it, MagicAbility.DoesNotUntap, MagicCondition.TAPPED_CONDITION)
                ));
            });
        }
    }
]
