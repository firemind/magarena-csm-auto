[
    new MagicWhenOtherComesIntoPlayTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPermanent otherPermanent) {
            return (otherPermanent.isCreature() &&
                    otherPermanent.hasType(MagicType.Legendary) &&
                    otherPermanent.isController(permanent.getController())) ?
                new MagicEvent(
                    permanent,
                    new MagicMayChoice(),
                    otherPermanent,
                    this,
                    "You may\$ attach SN to RN."
                ) :
                MagicEvent.NONE;
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isYes()) {
                game.doAction(new MagicAttachAction(
                    event.getPermanent(),
                    event.getRefPermanent()
                ));
            }
        }
    }
]
