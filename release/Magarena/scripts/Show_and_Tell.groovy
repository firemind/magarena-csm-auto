def ARTIFACT_CREATURE_ENCHANTMENT_OR_LAND_FROM_HAND = new MagicCardFilterImpl() {
    public boolean accept(final MagicSource source,final MagicPlayer player,final MagicCard target) {
        return target.hasType(MagicType.Artifact) || target.hasType(MagicType.Creature) ||target.hasType(MagicType.Land) || target.hasType(MagicType.Enchantment);
    }
    public boolean acceptType(final MagicTargetType targetType) {
        return targetType == MagicTargetType.Hand;
    }
};
def AN_ARTIFACT_CREATURE_ENCHANTMENT_OR_LAND_FROM_HAND = new MagicTargetChoice(
    ARTIFACT_CREATURE_ENCHANTMENT_OR_LAND_FROM_HAND,
    MagicTargetHint.None,
    "an artifact creature enchantment or land card from your hand"
);


def EVENT_ACTION = {
    final MagicGame game, final MagicEvent event ->
        event.processTargetCard(game, {
                game.doAction(new ReturnCardAction(MagicLocationType.OwnersHand,it,event.getPlayer()));
        
    });
}



[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
                    return new MagicEvent(
                    cardOnStack,
                    
                    
                    this,
                    "Each player puts an artifact, creature," + 
                    "enchantment or land card from " +
                    "his or her hand onto the battlefield."
                );
            }
        
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            for (final MagicPlayer player : game.getAPNAP()) {
                     game.addEvent(new MagicEvent(
                    event.getSource(),
                    player,
                    AN_ARTIFACT_CREATURE_ENCHANTMENT_OR_LAND_FROM_HAND,
                    MagicGraveyardTargetPicker.PutOntoBattlefield,
                    EVENT_ACTION,
                    "PN puts a an artifact, creature, enchantment or land card from his or her hand onto the battlefield."
                    
                    
                    
                ));
            }
        }
    }
]
