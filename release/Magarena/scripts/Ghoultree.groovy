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
            final int n = source.getController().filterCards(MagicTargetFilterFactory.CREATURE_CARD_FROM_GRAVEYARD).size();    
            return [
                new MagicPayManaCostEvent(
                    source,
                    source.getCost().reduce(MagicCostManaType.Colorless, n)
                )
            ];
        }
    }
]
