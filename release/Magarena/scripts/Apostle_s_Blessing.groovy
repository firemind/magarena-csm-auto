def ARTIFACT_OR_CREATURE_YOU_CONTROL = new MagicPermanentFilterImpl() {
    public boolean accept(final MagicGame game,final MagicPlayer player,final MagicPermanent target) {
        return target.isController(player) && (target.isCreature() || target.isArtifact());
    } 
};

def TARGET_ARTIFACT_OR_CREATURE_YOU_CONTROL = new MagicTargetChoice(
    ARTIFACT_OR_CREATURE_YOU_CONTROL,
    "target artifact or creature you control"
);


def ProtectionFromArtifacts = MagicAbility.getAbilityList("protection from artifacts");

def action = {
    final MagicGame game, final MagicEvent event ->
    game.doAction(new MagicGainAbilityAction(
        event.getRefPermanent(),
        event.getChosenColor().getProtectionAbility()
    ));
}

[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack, final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                new MagicOrChoice(
                    TARGET_ARTIFACT_OR_CREATURE_YOU_CONTROL,
                    TARGET_ARTIFACT_OR_CREATURE_YOU_CONTROL
                ),
                this,
                "Choose one\$ - target artifact or creature\$ you control gains protection from artifacts until end of turn; " +
                "or target artifact or creature\$ you control gains protection from the color of your choice until end of turn.\$" 
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isMode(1)) {
                event.processTargetPermanent(game, {
                    game.doAction(new MagicGainAbilityAction(it,ProtectionFromArtifacts))
                });
            }
            if (event.isMode(2)) {
                event.processTargetPermanent(game, {
                    game.addEvent(new MagicEvent(
                        event.getSource(),
                        event.getPlayer(),
                        MagicColorChoice.ALL_INSTANCE,
                        it,
                        action,
                        "Chosen color\$."
                    ));
                });
            }
        }
    }
]
