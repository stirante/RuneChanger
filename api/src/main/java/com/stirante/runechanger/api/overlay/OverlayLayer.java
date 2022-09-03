package com.stirante.runechanger.api.overlay;

import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.utils.Constants;
import com.stirante.runechanger.utils.SceneType;
import com.stirante.runechanger.utils.SimplePreferences;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public abstract class OverlayLayer implements MouseMotionListener, MouseListener, MouseWheelListener {
    protected static final Color TEXT_COLOR = new Color(0xc8aa6e);
    protected static final Color DARKER_TEXT_COLOR = new Color(0x785928);
    protected static final Color BACKGROUND_COLOR = new Color(0x010a13);
    protected static final Color GRADIENT_COLOR_1 = new Color(0x60491F);
    protected static final Color GRADIENT_COLOR_2 = new Color(0x463714);
    protected static final Color LIGHTEN_COLOR = new Color(1f, 1f, 1f, 0.2f);
    protected static final Color DIVIDER_COLOR = new Color(0x1e2328);
    protected static final Color DARKEN_COLOR = new Color(0f, 0f, 0f, 0.01f);

    private final ClientOverlay overlay;

    public OverlayLayer(ClientOverlay overlay) {
        this.overlay = overlay;
    }

    protected static float ease(float currentPosition, float targetPosition) {
        if (!SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.ENABLE_ANIMATIONS, true)) {
            return targetPosition;
        }
        return currentPosition + ((targetPosition - currentPosition) * 0.2f);
    }

    public int getZIndex() {
        return 0;
    }

    public void drawOverlay(Graphics g) {
        Image img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = img.getGraphics();
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setFont(getClientOverlay().getDefaultFont());
        draw(graphics);
        graphics.dispose();
        g.drawImage(img, 0, 0, null);
    }

    protected abstract void draw(Graphics g);

    protected ClientOverlay getClientOverlay() {
        return overlay;
    }

    protected SceneType getSceneType() {
        return getClientOverlay().getSceneType();
    }

    protected int getClientWidth() {
        return (int) (getWidth() - (Constants.EXTRA_WIDTH * getHeight()));
    }

    protected int getWidth() {
        return getClientOverlay().getWidth();
    }

    protected int getHeight() {
        return getClientOverlay().getHeight();
    }

    protected void repaintLater() {
        getClientOverlay().repaintLater();
    }

    protected void repaintNow() {
        getClientOverlay().repaint();
    }

    protected void clearRect(Graphics g, int x, int y, int w, int h) {
        getClientOverlay().clearRect(g, x, y, w, h);
    }

    protected RuneChangerApi getApi() {
        return getClientOverlay().getApi();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

    }
}
