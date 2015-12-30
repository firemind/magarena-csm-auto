package magic.ui.duel.player;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Set;
import javax.swing.JLabel;
import magic.data.GeneralConfig;
import magic.model.MagicPlayerZone;
import magic.ui.duel.SwingGameController;
import magic.ui.IChoiceViewer;
import magic.ui.duel.viewer.info.PlayerViewerInfo;
import magic.ui.theme.ThemeFactory;
import magic.ui.widget.PanelButton;
import magic.ui.widget.TexturedPanel;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class GamePlayerPanel extends TexturedPanel implements IChoiceViewer {
    
    private PlayerViewerInfo playerInfo;
    private PlayerZoneButtonsPanel zoneButtonsPanel;
    private PlayerImagePanel avatarPanel;
    private final PanelButton avatarButton;

    public GamePlayerPanel(final SwingGameController controller, final PlayerViewerInfo playerInfo) {

        this.playerInfo = playerInfo;

        setOpaque(false);
        setPreferredSize(new Dimension(0, 80));
        setMinimumSize(getPreferredSize());

        zoneButtonsPanel = new PlayerZoneButtonsPanel(playerInfo, controller);

        avatarPanel = new PlayerImagePanel(playerInfo);
        avatarPanel.setOpaque(false);

        avatarButton = new PanelButton() {
            @Override
            public Color getValidColor() {
                return ThemeFactory.getInstance().getCurrentTheme().getChoiceColor();
            }
            @Override
            public void mouseClicked() {
                controller.processClick(playerInfo.player);
            }
        };
        avatarButton.setComponent(avatarPanel);

        setLayout(new MigLayout("flowy, insets 0, gap 4 1, wrap 2"));
        add(avatarButton, "w 80!, h 80!, spany 2");
        add(getPlayerLabel(), "gaptop 3");
        add(zoneButtonsPanel, "w 100%, h 100%");

        if (controller != null) {
            controller.registerChoiceViewer(this);
        }

    }

    private JLabel getPlayerLabel() {
        final JLabel lbl = new JLabel(playerInfo.playerLabel);
        lbl.setFont(new Font("dialog", Font.PLAIN, 9));
        return lbl;
    }

    private boolean isThisPlayerValidChoice(Set<?> validChoices) {
        return !validChoices.isEmpty() && validChoices.contains(playerInfo.player);
    }

    @Override
    public void showValidChoices(Set<?> validChoices) {
        final boolean isValid = isThisPlayerValidChoice(validChoices);
        if (GeneralConfig.getInstance().showGameplayAnimations()) {
            avatarPanel.doPulsingBorderAnimation(isValid);
            avatarButton.setValidNoOverlay(isValid);
        } else {
            avatarButton.setValid(isValid);
        }
    }

    public void updateDisplay(final PlayerViewerInfo playerInfo) {
        this.playerInfo = playerInfo;
        avatarPanel.updateDisplay(playerInfo);
        zoneButtonsPanel.updateDisplay(playerInfo);
    }

    public void setActiveZone(MagicPlayerZone zone) {
        zoneButtonsPanel.setActiveZone(zone);
    }

    public PlayerViewerInfo getPlayerInfo() {
        return playerInfo;
    }

    public void doFlashPlayerHandZoneButton() {
        zoneButtonsPanel.doFlashPlayerHandZoneButton();
    }

    public void doFlashLibraryZoneButton() {
        zoneButtonsPanel.doFlashLibraryZoneButton();
    }

    public void doHighlightPlayerZone(MagicPlayerZone zone, boolean b) {
        zoneButtonsPanel.doHighlightPlayerZone(zone, b);
    }

    public Rectangle getLibraryButtonLayout(Component canvas) {
        return zoneButtonsPanel.getZoneButtonRectangle(MagicPlayerZone.LIBRARY, canvas);
    }

    public Rectangle getHandButtonLayout(Component canvas) {
        return zoneButtonsPanel.getZoneButtonRectangle(MagicPlayerZone.HAND, canvas);
    }

}
