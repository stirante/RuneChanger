package com.stirante.RuneChanger.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Map;

public class RuneButton extends JPanel {
    private final Map<?, ?> desktopHints;
    private final ButtonType type;
    private Image img = null;
    private Font mFont;

    public RuneButton(ButtonType type) {
        super();
        this.type = type;
        try {
            img = ImageIO.read(getClass().getResourceAsStream("/button.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mFont = Font.decode("Arial-BOLD-15");
        desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (desktopHints != null) {
            g2d.setRenderingHints(desktopHints);
        }
        if (img != null) {
            g2d.drawImage(img, 0, 0, Constants.WIDTH, Constants.HEIGHT, null);
        }
        g2d.setFont(mFont);
        g2d.setColor(Color.white);
        drawCenteredString(g2d, type.getMessage());
    }

    /**
     * Draws centered string
     * @param g graphics
     * @param text text to draw
     */
    private void drawCenteredString(Graphics g, String text) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = (Constants.WIDTH - metrics.stringWidth(text)) / 2;
        int y = ((Constants.HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        g.drawString(text, x, y);
    }
}
