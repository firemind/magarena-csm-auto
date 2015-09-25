[
    new MagicAtYourUpkeepTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPlayer upkeepPlayer) {
            return new MagicEvent(
                permanent,
                this,
                "PN puts X 2/2 black Zombie creature tokens onto the " +
                "battlefield, where X is half the number of Zombies you control, rounded down"
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final MagicPlayer player = event.getPlayer();
            game.doAction(new PlayTokensAction(
                player,
                CardDefinitions.getToken("2/2 black Zombie creature token"),
                player.getNrOfPermanents(MagicSubType.Zombie).intdiv(2)
            ));
        }
    }
]
