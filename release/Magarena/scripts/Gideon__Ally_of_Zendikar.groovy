def ABILITY2 = MagicRuleEventAction.create("Put a 2/2 white Knight Ally creature token onto the battlefield.");

def AB = new MagicStatic(MagicLayer.Ability, MagicStatic.UntilEOT) {
    @Override
    public void modAbilityFlags(final MagicPermanent source,final MagicPermanent permanent,final Set<MagicAbility> flags) {
        permanent.addAbility(MagicAbility.Indestructible, flags);
    }
};

def ST = new MagicStatic(MagicLayer.Type, MagicStatic.UntilEOT) {
    @Override
    public void modSubTypeFlags(final MagicPermanent permanent,final Set<MagicSubType> flags) {
        flags.add(MagicSubType.Human);
        flags.add(MagicSubType.Soldier);
    }
    @Override
    public int getTypeFlags(final MagicPermanent permanent,final int flags) {
        return flags|MagicType.Creature.getMask();
    }
};

def PreventAllDamage = new PreventDamageTrigger() {
    @Override
    public MagicEvent executeTrigger(final MagicGame game, final MagicPermanent permanent, final MagicDamage damage) {
        if (permanent == damage.getTarget()) {
            // Replacement effect. Generates no event or action.
            damage.prevent();
        }
        return MagicEvent.NONE;
    }
};

[
    new MagicPlaneswalkerActivation(1) {
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                this,
                "Until end of turn, Gideon, Ally of Zendikar becomes a 5/5 Human Soldier Ally creature " +
                "with indestructible. " +
                "He's still a planeswalker. Prevent all damage that would be dealt to him this turn."
            );
        }
        @Override
        public void executeEvent(final MagicGame game, final MagicEvent event) {
            final int amt = event.getPermanent().getCounters(MagicCounterType.Loyalty);
            def PT = new MagicStatic(MagicLayer.SetPT, MagicStatic.UntilEOT) {
                @Override
                public void modPowerToughness(final MagicPermanent source,final MagicPermanent permanent,final MagicPowerToughness pt) {
                    pt.set(5,5);
                }
            };

            game.doAction(new BecomesCreatureAction(event.getPermanent(),PT,AB,ST));
            game.doAction(new AddTurnTriggerAction(event.getPermanent(), PreventAllDamage));
        }
    },    
    new MagicPlaneswalkerActivation(0) {
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return ABILITY2.getEvent(source);
        }
    },
    new MagicPlaneswalkerActivation(-4) {
        @Override
        public MagicEvent getPermanentEvent(final MagicPermanent source,final MagicPayedCost payedCost) {
            return new MagicEvent(
                source,
                this,
                "PN gets an emblem with \"Creatures you control get +1/+1.\""
            );
        }
        @Override
        public void executeEvent(final MagicGame outerGame, final MagicEvent outerEvent) {
            final long pId = outerEvent.getPlayer().getId();
            outerGame.doAction(new AddStaticAction(
                new MagicStatic(MagicLayer.ModPT, ANY) {
                    @Override
                    public void modPowerToughness(final MagicPermanent source,final MagicPermanent permanent,final MagicPowerToughness pt) {
                        pt.add(1, 1);
                    }
                    @Override
                    public boolean condition(final MagicGame game,final MagicPermanent source,final MagicPermanent target) {
                        return target.getController().getId() == pId && target.isCreature();
                    }
                }
            ));
        }
    }
]
