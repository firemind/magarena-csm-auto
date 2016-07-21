def COLOR = {
    final MagicColor color ->
    new MagicStatic(MagicLayer.Color) {
        @Override
        public int getColorFlags(final MagicPermanent permanent,final int flags) {
            return color.getMask();
        }
    }
}

def action = {
    final MagicGame game, final MagicEvent event ->
    game.doAction(new AddStaticAction(event.getRefPermanent(), COLOR(event.getChosenColor())));
}

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Pump),
        "Color"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                    new MagicTapEvent(source),
                    new MagicPayManaCostEvent(source,"{2}")
                ];
        }
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                TARGET_PERMANENT_YOU_CONTROL,
                this,
                "Target permanent PN controls\$ becomes the color of his or her choice."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetPermanent(game, {
                game.addEvent(new MagicEvent(
                    event.getSource(),
                    event.getPlayer(),
                    MagicColorChoice.ALL_INSTANCE,
                    it,
                    action,
                    "Chosen color\$."
                ));
            });
        }
    }
]
