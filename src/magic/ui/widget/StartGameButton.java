package magic.ui.widget;

import javax.swing.AbstractAction;
import magic.translate.UiString;
import magic.ui.screen.widget.MenuButton;

@SuppressWarnings("serial")
public class StartGameButton extends MenuButton {

    private static final String _S1 = "Preparing game, please wait...";

    public StartGameButton(String text, AbstractAction action) {
        super(text, action);
    }

    public void setBusy(boolean b) {
        setEnabled(!b);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setToolTipText(b ? null : UiString.get(_S1));
    }
}
