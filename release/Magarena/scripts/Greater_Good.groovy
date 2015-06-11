[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Draw),
        "Draw"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [
                new MagicSacrificePermanentEvent(source,SACRIFICE_CREATURE)
            ];
        }

        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                this,
                payedCost.getTarget(),
                this,
                "PN draw cards equal to the power of RN then discard three cards."
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTarget(game, {
                final MagicPermanent sacrificed=event.getRefPermanent();
                game.doAction(new DrawAction(event.getSource(),it,sacrificed.getPower()));
        game.addEvent(new MagicDiscardEvent(event.getSource(),player,3));
            });
        }
    }
]