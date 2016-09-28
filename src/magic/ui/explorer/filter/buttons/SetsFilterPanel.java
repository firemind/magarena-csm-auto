package magic.ui.explorer.filter.buttons;

import java.util.stream.Stream;
import magic.data.MagicSetDefinitions;
import magic.data.MagicSets;
import magic.model.MagicCardDefinition;
import magic.translate.StringContext;
import magic.translate.UiString;
import magic.ui.explorer.filter.IFilterListener;

@SuppressWarnings("serial")
public class SetsFilterPanel extends CheckBoxFilterPanel {

    // translatable strings
    @StringContext(eg = "Set filter in Cards Explorer")
    private static final String _S1 = "Set";

    public SetsFilterPanel(IFilterListener aListener) {
        super(UiString.get(_S1), aListener);
    }

    @Override
    public boolean isCardValid(MagicCardDefinition card, int i) {
        return MagicSetDefinitions.isCardInSet(card, MagicSets.values()[i]);
    }

    @Override
    protected String[] getFilterValues() {
        return Stream.of(MagicSets.values())
                .map(s -> s.toString().replace("_", "") + " " + s.getSetName())
                .toArray(size -> new String[size]);
    }
}
