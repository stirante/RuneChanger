package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.InGameButton;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.util.RuneSelectedListener;
import com.stirante.RuneChanger.util.SimplePreferences;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuneButton extends JPanel {

    private static final boolean DISPLAY_FAKE = false;

    public static final String BOT = "BOT";
    public static final String MID = "MID";
    public static final String TOP = "TOP";
    private final Map<?, ?> desktopHints;
    private final List<RunePage> pages = new ArrayList<>();
    private RuneSelectedListener runeSelectedListener;
    private Font mFont;
    private boolean opened = false;
    private int selected = -1;
    private Color textColor = new Color(0xc8aa6e);
    private Color backgroundColor = new Color(0x010a13);
    private Color lightenColor = new Color(1f, 1f, 1f, 0.2f);
    private Color dividerColor = new Color(0x1e2328);
    private Color darkenColor = new Color(0f, 0f, 0f, 0.01f);
    private BufferedImage icon;
    private BufferedImage fake;
    private Rectangle botButton = new Rectangle(0, 0, 0, 0);
    private Rectangle midButton = new Rectangle(0, 0, 0, 0);
    private Rectangle topButton = new Rectangle(0, 0, 0, 0);

    public RuneButton() {
        super();
        try {
            InputStream is = getClass().getResourceAsStream("/Beaufort-Bold.ttf");
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            mFont = font.deriveFont(15f);
            icon = ImageIO.read(getClass().getResourceAsStream("/images/runechanger-runeforge-icon-28x28.png"));
            if (DISPLAY_FAKE)
                fake = ImageIO.read(new File("champ select.png"));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        setBackground(new Color(0f, 0f, 0f, 0f));
    }

    public void setRuneData(List<RunePage> pages, RuneSelectedListener runeSelectedListener) {
        this.runeSelectedListener = runeSelectedListener;
        this.pages.clear();
        this.pages.addAll(pages);
        repaint();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.clearRect(0, 0, getWidth(), getHeight());
        if (desktopHints != null) {
            g2d.setRenderingHints(desktopHints);
        }
        //fake image with champion selection for quick layout checking
        if (DISPLAY_FAKE)
            g2d.drawImage(fake, 0, 0, null);
        g2d.setFont(mFont);
        g2d.setColor(textColor);
        //draw rune button
        if (pages.size() > 0) {
            g2d.drawImage(icon, (int) (getWidth() * Constants.RUNE_BUTTON_POSITION_X), (int) (getHeight() * Constants.RUNE_BUTTON_POSITION_Y), null);
        }
        //draw rune menu
        if (opened) {
            g2d.setColor(backgroundColor);
            int menuX = (int) (Constants.RUNE_MENU_X * getWidth());
            int menuY = (int) ((Constants.RUNE_MENU_Y * getHeight()) - (Constants.RUNE_ITEM_HEIGHT * getHeight() * pages.size()));
            int itemWidth = (int) (Constants.RUNE_ITEM_WIDTH * getWidth());
            int itemHeight = (int) (Constants.RUNE_ITEM_HEIGHT * getHeight());
            int menuHeight = (int) (Constants.RUNE_ITEM_HEIGHT * getHeight() * pages.size());
            g2d.fillRect(menuX, menuY, itemWidth, menuHeight);
            g2d.setColor(textColor);
            g2d.drawRect(menuX, menuY, itemWidth, menuHeight);
            for (int i = 0; i < pages.size(); i++) {
                RunePage page = pages.get(i);
                int itemTop = (int) ((Constants.RUNE_MENU_Y - ((pages.size() - i) * Constants.RUNE_ITEM_HEIGHT)) * getHeight());
                int itemBottom = (int) ((Constants.RUNE_MENU_Y - ((pages.size() - i - 1) * Constants.RUNE_ITEM_HEIGHT)) * getHeight());
                if (selected == i) {
                    g2d.setColor(lightenColor);
                    g2d.fillRect(1 + menuX, itemTop, itemWidth, itemHeight);
                }
                g2d.setColor(textColor);
                g2d.drawImage(page.getRunes().get(0).getImage(), menuX, itemTop, itemHeight, itemHeight, null);
                drawCenteredHorizontalString(g2d, menuX + itemHeight, itemBottom, page.getName());
                if (i != pages.size() - 1) {
                    g2d.setColor(dividerColor);
                    g2d.drawLine(1 + menuX, itemBottom, (int) ((Constants.RUNE_MENU_X + Constants.RUNE_ITEM_WIDTH) * getWidth()) - 1, itemBottom);
                }
            }
        }
        //draw quick replies
        if (Boolean.parseBoolean(SimplePreferences.getValue("quickReplies"))) {
            int chatY = (int) (Constants.QUICK_CHAT_Y * getHeight());
            int chatX = (int) (Constants.QUICK_CHAT_X * getWidth());
            g2d.rotate(Math.toRadians(-90), chatX, chatY);

            int botWidth = g2d.getFontMetrics().stringWidth(BOT);
            int midWidth = g2d.getFontMetrics().stringWidth(MID);
            int topWidth = g2d.getFontMetrics().stringWidth(TOP);
            int bgHeight = g2d.getFontMetrics().getHeight() - (g2d.getFontMetrics().getAscent() / 2);

            g2d.setColor(darkenColor);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() + (g2d.getFontMetrics().getAscent() / 2), botWidth, bgHeight);
            botButton.x = (int) (Constants.QUICK_CHAT_X * getWidth()) - g2d.getFontMetrics().getHeight() + (g2d.getFontMetrics().getAscent() / 2);
            botButton.y = chatY - botWidth;
            botButton.width = bgHeight;
            botButton.height = botWidth;
            g2d.setColor(textColor);
            g2d.drawString(BOT, chatX, chatY);
            chatX += botWidth + Constants.MARGIN;

            g2d.setColor(darkenColor);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() + (g2d.getFontMetrics().getAscent() / 2), midWidth, bgHeight);
            midButton.x = botButton.x;
            midButton.y = chatY - botWidth - Constants.MARGIN - midWidth;
            midButton.width = bgHeight;
            midButton.height = midWidth;
            g2d.setColor(textColor);
            g2d.drawString(MID, chatX, chatY);
            chatX += midWidth + Constants.MARGIN;

            g2d.setColor(darkenColor);
            g2d.fillRect(chatX, chatY - g2d.getFontMetrics().getHeight() + (g2d.getFontMetrics().getAscent() / 2), topWidth, bgHeight);
            topButton.x = botButton.x;
            topButton.y = chatY - botWidth - Constants.MARGIN - midWidth - Constants.MARGIN - topWidth;
            topButton.width = bgHeight;
            topButton.height = topWidth;
            g2d.setColor(textColor);
            g2d.drawString(TOP, chatX, chatY);
        }
    }

    private void drawCenteredHorizontalString(Graphics2D g, int x, int bottom, String text) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        if (metrics.stringWidth(text) > (Constants.RUNE_ITEM_WIDTH * getWidth()) - (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
            while (metrics.stringWidth(text) > (Constants.RUNE_ITEM_WIDTH * getWidth()) - (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
                text = text.substring(0, text.length() - 1);
            }
            text = text.substring(0, text.length() - 2) + "...";
        }
        g.drawString(text, x - 1, bottom - metrics.getHeight() + (metrics.getAscent() / 2));
    }

    private float lastX = 0f;
    private float lastY = 0f;

    public void mouseClicked(MouseEvent e) {
        if (DISPLAY_FAKE) {
            float x = ((float) e.getX()) / ((float) getWidth());
            float y = ((float) e.getY()) / ((float) getHeight());
            if (e.getButton() == MouseEvent.BUTTON1) {
                lastX = x;
                lastY = y;
                System.out.println(x + " x " + y);
            } else {
                System.out.println("Distance: " + (x - lastX) + " x " + (y - lastY));
            }
        }
        if (e.getX() > (getWidth() * Constants.RUNE_BUTTON_POSITION_X) &&
                e.getX() < (getWidth() * Constants.RUNE_BUTTON_POSITION_X) + icon.getWidth() &&
                e.getY() > (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) &&
                e.getY() < (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) + icon.getHeight()) {
            opened = !opened;
        } else if (selected != -1) {
            runeSelectedListener.onRuneSelected(pages.get(selected));
            opened = !opened;
        } else {
            if (botButton.contains(e.getX(), e.getY())) {
                InGameButton.sendMessageToChampSelect(BOT.toLowerCase());
            } else if (midButton.contains(e.getX(), e.getY())) {
                InGameButton.sendMessageToChampSelect(MID.toLowerCase());
            } else if (topButton.contains(e.getX(), e.getY())) {
                InGameButton.sendMessageToChampSelect(TOP.toLowerCase());
            }
        }
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        int i;
        if (e.getY() > Constants.RUNE_MENU_Y * getWidth() || e.getY() < (Constants.RUNE_MENU_Y - pages.size()) * getHeight()
                || e.getX() < Constants.RUNE_MENU_X * getWidth() || e.getX() > (Constants.RUNE_MENU_X + Constants.RUNE_ITEM_WIDTH) * getWidth())
            i = -1;
        else
            i = (int) ((e.getY() - ((Constants.RUNE_MENU_Y - (Constants.RUNE_ITEM_HEIGHT * pages.size())) * getHeight())) / (Constants.RUNE_ITEM_HEIGHT * getHeight()));
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
