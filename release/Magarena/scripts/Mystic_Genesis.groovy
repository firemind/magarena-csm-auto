[
    new MagicSpellCardEvent() {
        @Override
        public MagicEvent getEvent(final MagicCardOnStack cardOnStack,final MagicPayedCost payedCost) {
            return new MagicEvent(
                cardOnStack,
                TARGET_SPELL,
                this,
                "Counter target spell. Put an X/X green Ooze creature token onto the battlefield, where X is that spell's converted mana cost."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            event.processTargetCardOnStack(game, {
                final MagicCardOnStack card ->
                game.doAction(new CounterItemOnStackAction(card));

                final int x = card.getConvertedCost();
                game.doAction(new PlayTokenAction(event.getPlayer(), MagicCardDefinition.create({
                    it.setName("Ooze");
                    it.setDistinctName("green Ooze creature token");
                    it.setPowerToughness(x, x);
                    it.setColors("g");
                    it.addSubType(MagicSubType.Ooze);
                    it.addType(MagicType.Creature);
                    it.setToken();
                    it.setValue(x);
                })));
            })
        }
    }
]
