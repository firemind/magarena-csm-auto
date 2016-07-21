[
    new ThisDiesTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game, final MagicPermanent permanent, final MagicPermanent died) {
            return new MagicEvent(
                permanent,
                permanent.getPower(),
                this,
                "PN puts an X/X black Horror creature token onto the battlefield, where X is SN's power. (RN)"
            );
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final int x = event.getRefInt();
            game.doAction(new PlayTokenAction(
                event.getPlayer(),
                MagicCardDefinition.create(
                    CardDefinitions.getToken("black Horror creature token"),
                    {
                        it.setPowerToughness(x, x);
                        it.setValue(x);
                    }
                )
            ));
        }
    }
]
