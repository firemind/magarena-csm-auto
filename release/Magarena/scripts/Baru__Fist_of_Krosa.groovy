def CARD_NAMED_BARU = new MagicCardFilterImpl() {
    public boolean accept(final MagicGame game,final MagicPlayer player,final MagicCard target) {
        return target.getName().equals("Baru, Fist of Krosa");
    }
    public boolean acceptType(final MagicTargetType targetType) {
        return targetType == MagicTargetType.Hand;
    }
}; 
def A_CARD_NAMED_BARU = new MagicTargetChoice(
    CARD_NAMED_BARU,  
    MagicTargetHint.None,
    "a card named Baru, Fist of Krosa from your hand"
);

[
    new MagicPermanentActivation(
        new MagicActivationHints(MagicTiming.Pump),
        "Grandeur"
    ) {
        @Override
        public Iterable<MagicEvent> getCostEvent(final MagicPermanent source) {
            return [new MagicDiscardChosenEvent(source,A_CARD_NAMED_BARU)];
        }
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                this,
                "Put an X/X green Wurm creature token onto the battlefield, where X is the number of lands you control."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final int x = event.getPlayer().getNrOfPermanents(MagicType.Land);
            game.doAction(new MagicPlayTokenAction(event.getPlayer(), MagicCardDefinition.create({
                it.setName("Wurm");
                it.setFullName("green Wurm creature token");
                it.setPowerToughness(x, x);
                it.setColors("g");
                it.addSubType(MagicSubType.Wurm);
                it.addType(MagicType.Creature);
                it.setToken();
                it.setValue(x);
            })));
        }
    }
]
