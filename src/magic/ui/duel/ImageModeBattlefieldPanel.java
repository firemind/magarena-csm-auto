package magic.ui.duel;

import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import magic.model.MagicCardList;
import magic.ui.duel.resolution.ResolutionProfileResult;
import magic.ui.duel.resolution.ResolutionProfileType;
import magic.ui.duel.viewer.ImageBattlefieldViewer;
import magic.ui.duel.viewer.ImageCombatViewer;
import magic.ui.duel.viewer.PlayerZoneViewer;
import magic.ui.duel.sidebar.StackViewer;
import magic.ui.duel.viewer.info.CardViewerInfo;

@SuppressWarnings("serial")
public class ImageModeBattlefieldPanel extends BattlefieldPanel {

    private final BattlefieldTextOverlay textOverlay = new BattlefieldTextOverlay();

    private final PlayerZoneViewer playerZoneViewer;
    public final ImageBattlefieldViewer imagePlayerPermanentViewer;
    public final ImageBattlefieldViewer imageOpponentPermanentViewer;
    private final ImageCombatViewer imageCombatViewer;
    private final StackViewer imageStackViewer;

    public ImageModeBattlefieldPanel(final SwingGameController controller) {

        playerZoneViewer = controller.getPlayerZoneViewer();
        imagePlayerPermanentViewer = new ImageBattlefieldViewer(controller, false);
        imageOpponentPermanentViewer = new ImageBattlefieldViewer(controller, true);
        imageCombatViewer = new ImageCombatViewer(controller);
        imageStackViewer = new StackViewer(controller);

        playerZoneViewer.addPropertyChangeListener(PlayerZoneViewer.CP_PLAYER_ZONE,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        textOverlay.setPlayerZoneName((String) evt.getNewValue());
                        repaint();
                    }
                });

        setLayout(null);
        add(imageStackViewer);
        add(playerZoneViewer);
        add(imagePlayerPermanentViewer);
        add(imageOpponentPermanentViewer);
        add(imageCombatViewer);

        setOpaque(false);
    }

    @Override
    public void doUpdate() {
        playerZoneViewer.update();
        imagePlayerPermanentViewer.update();
        imageOpponentPermanentViewer.update();
        imageCombatViewer.update();
        imageStackViewer.update();
    }

    @Override
    public void showCards(final MagicCardList cards) {
        playerZoneViewer.showCards(cards);
        playerZoneViewer.setSelectedTab(5);
    }

    @Override
    public void focusViewers(int handGraveyard) {
        playerZoneViewer.setSelectedTab(handGraveyard);
    }

    @Override
    public void resizeComponents(ResolutionProfileResult result) {
        playerZoneViewer.setBounds(result.getBoundary(ResolutionProfileType.GameImageHandGraveyardViewer));
        imagePlayerPermanentViewer.setBounds(result.getBoundary(ResolutionProfileType.GameImagePlayerPermanentViewer));
        imageOpponentPermanentViewer.setBounds(result.getBoundary(ResolutionProfileType.GameImageOpponentPermanentViewer));
        imageCombatViewer.setBounds(result.getBoundary(ResolutionProfileType.GameImageCombatViewer));
    }

    @Override
    public StackViewer getStackViewer() {
        return imageStackViewer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Image overlayImage = textOverlay.getOverlayImage(getWidth(), getHeight());
        g.drawImage(overlayImage, 0, 0, this);
    }

    @Override
    public void highlightCard(CardViewerInfo cardInfo, boolean b) {
        if (!imagePlayerPermanentViewer.highlightCard(cardInfo, b))
            if (!imageOpponentPermanentViewer.highlightCard(cardInfo, b))
                imageCombatViewer.highlightCard(cardInfo, b);
    }

}

