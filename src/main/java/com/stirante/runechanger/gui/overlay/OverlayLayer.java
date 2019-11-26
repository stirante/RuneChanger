package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public abstract class OverlayLayer implements MouseMotionListener, MouseListener, MouseWheelListener {
    protected static final Color TEXT_COLOR = new Color(0xc8aa6e);
    protected static final Color DARKER_TEXT_COLOR = new Color(0x785928);
    protected static final Color BACKGROUND_COLOR = new Color(0x010a13);
    protected static final Color LIGHTEN_COLOR = new Color(1f, 1f, 1f, 0.2f);
    protected static final Color DIVIDER_COLOR = new Color(0x1e2328);
    protected static final Color DARKEN_COLOR = new Color(0f, 0f, 0f, 0.01f);

    private final ClientOverlay overlay;

    public OverlayLayer(ClientOverlay overlay) {
        this.overlay = overlay;
    }

    protected static float ease(float currentPosition, float targetPosition) {
        return currentPosition + ((targetPosition - currentPosition) * 0.2f);
    }

    public int getZIndex() {
        return 0;
    }

    public void drawOverlay(Graphics g) {
        Image img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = img.getGraphics();
        getClientOverlay().initGraphics(graphics);
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
        return (int) (getWidth() - (Constants.CHAMPION_SUGGESTION_WIDTH * getHeight()));
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

    protected RuneChanger getRuneChanger() {
        return getClientOverlay().runeChanger;
    }

    protected void drawCenteredHorizontalString(Graphics2D g, int x, int bottom, String text) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        //we subtract item height, so we leave space for rune icon
        if (metrics.stringWidth(text) >
                (Constants.RUNE_ITEM_WIDTH * (getClientWidth())) - (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
            while (metrics.stringWidth(text) >
                    (Constants.RUNE_ITEM_WIDTH * (getClientWidth())) - (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
                text = text.substring(0, text.length() - 1);
            }
            text = text.substring(0, text.length() - 2) + "...";
        }
        g.drawString(text, x - 1, bottom - metrics.getHeight() + (metrics.getAscent() / 2));
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
