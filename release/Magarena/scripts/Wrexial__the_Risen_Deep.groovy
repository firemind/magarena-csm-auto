[
    new MagicWhenSelfCombatDamagePlayerTrigger() {
        @Override
        public MagicEvent executeTrigger(final MagicGame game,final MagicPermanent permanent,final MagicDamage damage) {
            return new MagicEvent(
                permanent,
                new MagicMayChoice(
                    TARGET_INSTANT_OR_SORCERY_CARD_FROM_OPPONENTS_GRAVEYARD
                ),
                MagicGraveyardTargetPicker.PutOntoBattlefield,
                this,
                "PN may\$ cast target instant or sorcery card\$ from your opponent's graveyard without paying its mana cost. "+
                "If that card would be put into a graveyard this turn, exile it instead."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            if (event.isYes()) {
                event.processTargetCard(game, {
                    game.doAction(CastCardAction.WithoutManaCost(
                        event.getPlayer(),
                        it,
                        MagicLocationType.Graveyard,
                        MagicLocationType.Exile
                    ));
                });
            }
        }
    }
]
