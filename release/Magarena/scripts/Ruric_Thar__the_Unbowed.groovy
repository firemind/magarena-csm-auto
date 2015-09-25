[
    new MagicWhenOtherSpellIsCastTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicCardOnStack cardOnStack) {
            return (!cardOnStack.hasType(MagicType.Creature)) ?
                new MagicEvent(
                    permanent,
                    cardOnStack.getController(),
                    this,
                    "SN deals 6 damage to PN."
                ):
                MagicEvent.NONE;
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new DealDamageAction(event.getSource(),event.getPlayer(),6));
        }
    }
]
