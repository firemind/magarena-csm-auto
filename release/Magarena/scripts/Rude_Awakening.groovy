def PT = new MagicStatic(MagicLayer.SetPT, MagicStatic.UntilEOT) {
    @Override
    public void modPowerToughness(final MagicPermanent source,final MagicPermanent permanent,final MagicPowerToughness pt) {
        pt.set(2,2);
    }
};

def ST = new MagicStatic(MagicLayer.Type, MagicStatic.UntilEOT) {
    @Override
    public int getTypeFlags(final MagicPermanent permanent,final int flags) {
        return flags|MagicType.Creature.getMask();
    }
};

def TEXT1 = "Untap all lands you control."

def TEXT2 = "Until end of turn, lands you control become 2/2 creatures that are still lands."

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                payedCost.isKicked() ? 
                    MagicChoice.NONE :
                    new MagicOrChoice(
                        MagicChoice.NONE,
                        MagicChoice.NONE
                    ),
                this,
                payedCost.isKicked() ?
                    TEXT1 + " " + TEXT2 :
                    "Choose one\$ — • " + TEXT1 + " • " + TEXT2
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isKicked() || event.isMode(1)) {
                final Collection<MagicPermanent> targets = event.getPlayer().filterPermanents(MagicTargetFilterFactory.LAND_YOU_CONTROL); 
                for (final MagicPermanent target : targets) {
                    game.doAction(new MagicUntapAction(target));
                }         
            } 
            if (event.isKicked() || event.isMode(2)) {
                final Collection<MagicPermanent> targets = event.getPlayer().filterPermanents(MagicTargetFilterFactory.LAND_YOU_CONTROL); 
                for (final MagicPermanent target : targets) {
                    game.doAction(new MagicBecomesCreatureAction(target,PT,ST));
                }         
            }
        }
    }
]
