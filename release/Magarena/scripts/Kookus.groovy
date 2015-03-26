def KEEPER_OF_KOOKUS = new MagicPermanentFilterImpl() {
    public boolean accept(final MagicGame game,final MagicPlayer player,final MagicPermanent target) {
        return target.isCreature() && target.getName().equals("Keeper of Kookus");
    } 
};

[
    new MagicAtYourUpkeepTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicPlayer upkeepPlayer) {
            return upkeepPlayer.controlsPermanent(KEEPER_OF_KOOKUS) == false ?
                new MagicEvent(
                    permanent,
                    this,
                    "SN deals 3 damage to PN and attacks this turn if able."
            ):
            MagicEvent.NONE;
        }

        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            game.doAction(new MagicDealDamageAction(event.getPermanent(),event.getPlayer(),3));
            game.doAction(new MagicGainAbilityAction(event.getPermanent(),MagicAbility.AttacksEachTurnIfAble));
        }
    }
]
