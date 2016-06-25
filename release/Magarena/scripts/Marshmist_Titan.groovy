[
     new MagicCardActivation(
        [MagicCondition.CARD_CONDITION],
        new MagicActivationHints(MagicTiming.Main, true),
        "Cast"
    ) {
        @Override
        public void change(final MagicCardDefinition cdef) {
            cdef.setCardAct(this);
        }

        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicCard source) {
            final int n = source.getController().getDevotion(MagicColor.Black);
            final int cost= Math.max(0,6-n)
            return cost==0 ?
                [new MagicPayManaCostEvent(source,"{B}")]:
                [new MagicPayManaCostEvent(source,"{"+cost.toString()+"}{B}")];
        }
    }
]