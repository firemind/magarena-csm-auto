[
    new MagicWhenSelfDamagePlayerTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicDamage damage) {
            return new MagicEvent(
                permanent,
                damage.getTargetPlayer(),
                this,
                "PN gets a poison counter."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new ChangePoisonAction(event.getPlayer(),1));
        }
    }
]
