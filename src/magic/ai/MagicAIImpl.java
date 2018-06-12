package magic.ai;

public enum MagicAIImpl {
    MMAB("minimax", new MMAB(false)),
    MMABC("minimax (cheat)", new MMAB(true)),
    MCTS("monte carlo tree search", new MCTSAI(false, false, false)),
    AMCTS("alt. monte carlo tree search", new MCTSAI(false, false, true)),
    MCTSC("monte carlo tree search (cheat)", new MCTSAI(true, false, false)),
    MCTSL("monte carlo tree search with combat logging", new MCTSAI(false, true, false)),
    GMCTS("guided monte carlo tree search", new GMCTSAI(false, false, false)),
    GAMCTS("guided alt. monte carlo tree search", new GMCTSAI(false, false, true)),
    GMCTSL("guided monte carlo tree search logging", new GMCTSAI(false, true, false)),
    GAMCTSL("guided alt. mcts logging", new GMCTSAI(false, true, true)),
    VEGAS("vegas", new VegasAI(false)),
    VEGASC("vegas (cheat)", new VegasAI(true)),
    MTDF("mtd(f)", new MTDF(false)),
    MTDFC("mtd(f) (cheat)", new MTDF(true)),
    FiremindAI("firemind(f)", new FiremindAI(false)),

    MMABFast("minimax (deck strength)", magic.ai.MMAB.DeckStrAI()),
    ;

    public static final MagicAIImpl[] SUPPORTED_AIS = {MMAB, MMABC, MCTS, MCTSC, MCTSL, VEGAS, VEGASC, FiremindAI};
    public static final MagicAIImpl[] DECKSTR_AIS = {MMABFast, MMABFast};

    private final String name;
    private final MagicAI ai;

    private MagicAIImpl(final String name, final MagicAI ai) {
        this.name=name;
        this.ai=ai;
    }

    public MagicAI getAI() {
        return ai;
    }

    @Override
    public String toString() {
        return name;
    }

}
