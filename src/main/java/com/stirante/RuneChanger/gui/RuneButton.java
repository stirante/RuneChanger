package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.util.RuneSelectedListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuneButton extends JPanel {
    private final Map<?, ?> desktopHints;
    private final List<RunePage> pages = new ArrayList<>();
    private final RuneSelectedListener runeSelectedListener;
    private Font mFont;
    private boolean opened = false;
    private int selected = -1;
    private Color textColor = new Color(0xc8aa6e);
    private Color backgroundColor = new Color(0x010a13);
    private Color lightenColor = new Color(1f, 1f, 1f, 0.2f);
    private Color dividerColor = new Color(0x1e2328);
    private BufferedImage icon;

    public RuneButton(List<RunePage> pages, RuneSelectedListener runeSelectedListener) {
        super();
        this.runeSelectedListener = runeSelectedListener;
        this.pages.addAll(pages);
        try {
            InputStream is = getClass().getResourceAsStream("/Beaufort-Bold.ttf");
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            mFont = font.deriveFont(15f);
            icon = ImageIO.read(getClass().getResourceAsStream("/images/runechanger-runeforge-icon-28x28.png"));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        setBackground(new Color(0f, 0f, 0f, 0f));
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.clearRect(0, 0, getWidth(), getHeight());
        if (desktopHints != null) {
            g2d.setRenderingHints(desktopHints);
        }
        g2d.setFont(mFont);
        g2d.setColor(textColor);
        g2d.drawImage(icon, Constants.MARGIN, getHeight() - Constants.MARGIN - Constants.ICON_SIZE, Constants.ICON_SIZE, Constants.ICON_SIZE, null);
        if (opened) {
            g2d.setColor(backgroundColor);
            g2d.fillRect(Constants.MARGIN + Constants.ELEMENT_OFFSET_X, Constants.MARGIN, Constants.ELEMENT_WIDTH, Constants.ELEMENT_HEIGHT * pages.size());
            g2d.setColor(textColor);
            g2d.drawRect(Constants.MARGIN + Constants.ELEMENT_OFFSET_X, Constants.MARGIN, Constants.ELEMENT_WIDTH, Constants.ELEMENT_HEIGHT * pages.size());
            for (int i = 0; i < pages.size(); i++) {
                RunePage page = pages.get(i);
                if (selected == i) {
                    g2d.setColor(lightenColor);
                    g2d.fillRect(1 + Constants.MARGIN + Constants.ELEMENT_OFFSET_X, i * Constants.ELEMENT_HEIGHT + Constants.MARGIN, Constants.ELEMENT_WIDTH, Constants.ELEMENT_HEIGHT);
                }
                g2d.setColor(textColor);
                g2d.drawImage(page.getRunes().get(0).getImage(), Constants.MARGIN + Constants.ELEMENT_OFFSET_X, i * Constants.ELEMENT_HEIGHT + Constants.MARGIN, Constants.ELEMENT_HEIGHT, Constants.ELEMENT_HEIGHT, null);
                drawCenteredHorizontalString(g2d, Constants.ELEMENT_HEIGHT + Constants.ELEMENT_OFFSET_X, (i + 1) * Constants.ELEMENT_HEIGHT + Constants.MARGIN, page.getName());
                if (i != pages.size() - 1) {
                    g2d.setColor(dividerColor);
                    g2d.drawLine(1 + Constants.MARGIN + Constants.ELEMENT_OFFSET_X, (i + 1) * Constants.ELEMENT_HEIGHT + Constants.MARGIN, getWidth() - 2 - Constants.MARGIN, (i + 1) * Constants.ELEMENT_HEIGHT + Constants.MARGIN);
                }
            }
        }
    }

    private void drawCenteredHorizontalString(Graphics2D g, int x, int bottom, String text) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        if (metrics.stringWidth(text) > Constants.ELEMENT_WIDTH - Constants.ELEMENT_HEIGHT) {
            while (metrics.stringWidth(text) > Constants.ELEMENT_WIDTH - Constants.ELEMENT_HEIGHT) {
                text = text.substring(0, text.length() - 1);
            }
            text = text.substring(0, text.length() - 2) + "...";
        }
        g.drawString(text, x + Constants.MARGIN, bottom - metrics.getHeight() + (metrics.getAscent() / 2));
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getX() > Constants.MARGIN && e.getX() < Constants.MARGIN + icon.getWidth() && e.getY() > getHeight() - Constants.MARGIN - icon.getHeight() && e.getY() < getHeight() - Constants.MARGIN)
            opened = !opened;
        else if (selected != -1) {
            runeSelectedListener.onRuneSelected(pages.get(selected));
            opened = !opened;
        }
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        int i;
        if (e.getY() < Constants.MARGIN || e.getY() > getHeight() - Constants.MARGIN)
            i = -1;
        else if (e.getX() < Constants.MARGIN + Constants.ELEMENT_OFFSET_X || e.getX() > getWidth() - Constants.MARGIN) i = -1;
        else i = (e.getY() - Constants.MARGIN) / Constants.ELEMENT_HEIGHT;
        if (i != selected) {
            selected = i;
            repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        if (selected != -1) {
            selected = -1;
            repaint();
        }
    }
}
