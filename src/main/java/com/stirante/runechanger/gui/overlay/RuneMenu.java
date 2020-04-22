package com.stirante.runechanger.gui.overlay;

import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.AnalyticsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RuneMenu extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(RuneMenu.class);
    private final List<RunePage> pages = new ArrayList<>();
    private Consumer<RunePage> runeSelectedListener;
    private boolean opened = false;
    private int selectedRunePageIndex = -1;
    private BufferedImage icon;
    private BufferedImage grayscaleIcon;
    private float currentRuneMenuPosition = 0f;
    private float scroll = 0f;

    public RuneMenu(ClientOverlay overlay) {
        super(overlay);
        try {
            icon = ImageIO.read(getClass().getResourceAsStream("/images/28.png"));
            grayscaleIcon = ImageIO.read(getClass().getResourceAsStream("/images/28grayscale.png"));
        } catch (IOException e) {
            log.error("Exception occurred while loading rune button icons", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading rune button icons", false);
        }
    }

    @Override
    protected void draw(Graphics g) {
        if (getSceneType() == SceneType.CHAMPION_SELECT) {
            drawRuneButton((Graphics2D) g);
            drawRuneMenu((Graphics2D) g);
        }
    }

    public void setRuneData(List<RunePage> pages, Consumer<RunePage> runeSelectedListener) {
        this.runeSelectedListener = runeSelectedListener;
        this.pages.clear();
        this.pages.addAll(pages);
        repaintNow();
    }

    private void drawRuneButton(Graphics2D g2d) {
        if (pages.size() > 0) {
            g2d.drawImage(icon, (int) ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X), (int) (getHeight() *
                    Constants.RUNE_BUTTON_POSITION_Y), (int) (Constants.RUNE_BUTTON_SIZE * getHeight()), (int) (
                    Constants.RUNE_BUTTON_SIZE * getHeight()), null);
        }
        else {
            g2d.drawImage(grayscaleIcon, (int) ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X), (int) (
                    getHeight() *
                            Constants.RUNE_BUTTON_POSITION_Y), (int) (Constants.RUNE_BUTTON_SIZE * getHeight()), (int) (
                    Constants.RUNE_BUTTON_SIZE * getHeight()), null);
            opened = false;
        }
    }

    private void drawRuneMenu(Graphics2D g2d) {
        //open/close animations
        if (opened) {
            currentRuneMenuPosition = ease(currentRuneMenuPosition, 100f);
            if (currentRuneMenuPosition < 99f) {
                repaintLater();
            }
            else {
                currentRuneMenuPosition = 100f;
            }
        }
        else {
            currentRuneMenuPosition = ease(currentRuneMenuPosition, 0f);
            scroll = ease(scroll, 0f);
            if (currentRuneMenuPosition > 1f || scroll > 1f) {
                repaintLater();
            }
            else {
                currentRuneMenuPosition = 0f;
                scroll = 0f;
            }
        }
        //positions and dimensions
        int itemWidth = (int) (Constants.RUNE_ITEM_WIDTH * (getClientWidth()));
        int itemHeight = (int) (Constants.RUNE_ITEM_HEIGHT * getHeight());
        int menuHeight = (int) (Constants.RUNE_ITEM_HEIGHT * getHeight() * pages.size());
        int menuX = (int) (Constants.RUNE_MENU_X * (getClientWidth()));
        int menuY = (int) ((Constants.RUNE_MENU_Y * getHeight()) -
                (Math.min(10 * itemHeight, itemHeight * pages.size()) * (currentRuneMenuPosition / 100f)) - scroll);

        //draw menu background
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(menuX,
                menuY, itemWidth,
                menuHeight);
        g2d.setColor(TEXT_COLOR);
        g2d.drawRect(menuX, menuY, itemWidth, menuHeight);

        //draw menu items
        for (int i = 0; i < pages.size(); i++) {
            RunePage page = pages.get(i);
            int itemTop = (int) ((menuY + (i * Constants.RUNE_ITEM_HEIGHT * getHeight())));
            int itemBottom = itemTop + itemHeight;
            //highlight hovered item
            if (selectedRunePageIndex == i) {
                g2d.setColor(LIGHTEN_COLOR);
                g2d.fillRect(1 + menuX, itemTop, itemWidth, itemHeight);
            }
            g2d.setColor(TEXT_COLOR);
            g2d.drawImage(page.getRunes().get(0).getImage(), menuX, itemTop, itemHeight, itemHeight, null);
            drawRuneText(g2d, menuX + itemHeight, itemBottom, page.getName(), page.getSourceName());
            //draw dividers, except at the bottom
            if (i != pages.size() - 1) {
                g2d.setColor(DIVIDER_COLOR);
                g2d.drawLine(
                        1 + menuX, itemBottom,
                        (int) ((Constants.RUNE_MENU_X + Constants.RUNE_ITEM_WIDTH) * (getClientWidth())) -
                                1, itemBottom);
            }
        }
        //clear everything under menu
        clearRect(g2d, menuX, (int) (Constants.RUNE_MENU_Y * getHeight()),
                itemWidth + 1, getHeight() - (int) (Constants.RUNE_MENU_Y * getHeight()));
        //clear everything above menu
        int upperY = (int) (Math.min(
                Constants.RUNE_ITEM_HEIGHT * 10 - (1 - Constants.RUNE_MENU_Y),
                Constants.RUNE_ITEM_HEIGHT * pages.size() - (1 - Constants.RUNE_MENU_Y)) * getHeight());
        clearRect(g2d, menuX, 0, itemWidth + 1, upperY);
        //draw top line, if menu is scrollable and menu is scrolled
        g2d.setColor(TEXT_COLOR);
        if (pages.size() > 10 && opened && scroll > 0f) {
            g2d.drawLine(menuX, upperY, menuX + itemWidth, upperY);
        }
    }

    protected void drawRuneText(Graphics2D g, int x, int bottom, String runeName, String runeSource) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        //we subtract item height, so we leave space for rune icon
        if (metrics.stringWidth(runeName) >
                (Constants.RUNE_ITEM_WIDTH * (getClientWidth())) - (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
            while (metrics.stringWidth(runeName) >
                    (Constants.RUNE_ITEM_WIDTH * (getClientWidth())) -
                            (Constants.RUNE_ITEM_HEIGHT * getHeight())) {
                runeName = runeName.substring(0, runeName.length() - 1);
            }
            runeName = runeName.substring(0, runeName.length() - 2) + "...";
        }
        g.drawString(runeName, x - 1, bottom - (metrics.getHeight() / 2) - metrics.getAscent());
        if (runeSource != null) {
            Color color = g.getColor();
            g.setColor(DARKER_TEXT_COLOR);
            g.drawString(runeSource,
                    x - 1,
                    bottom - (metrics.getHeight() / 2F));
            g.setColor(color);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (pages.size() > 0 && e.getX() > ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) &&
                e.getX() < ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) + icon.getWidth() &&
                e.getY() > (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) &&
                e.getY() < (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) + icon.getHeight()) {
            opened = !opened;
        }
        else if (selectedRunePageIndex != -1 && runeSelectedListener != null) {
            runeSelectedListener.accept(pages.get(selectedRunePageIndex));
            opened = !opened;
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        scroll += e.getUnitsToScroll() * e.getScrollAmount();
        scroll = (int) Math.max(0, Math.min(scroll, (pages.size() - 10) * Constants.RUNE_ITEM_HEIGHT * getHeight()));
    }

    public void mouseExited(MouseEvent e) {
        if (selectedRunePageIndex != -1) {
            selectedRunePageIndex = -1;
            repaintNow();
        }
    }

    public void mouseMoved(MouseEvent e) {
        int runePageIndex;
        if (pages.size() > 0 && e.getX() > ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) &&
                e.getX() < ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) + icon.getWidth() &&
                e.getY() > (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) &&
                e.getY() < (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) + icon.getHeight()) {
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        if (e.getY() > Constants.RUNE_MENU_Y * getClientWidth() ||
                e.getY() < Math.min(
                        Constants.RUNE_ITEM_HEIGHT * 10 - (1 - Constants.RUNE_MENU_Y),
                        Constants.RUNE_ITEM_HEIGHT * pages.size() - (1 - Constants.RUNE_MENU_Y)) *
                        getHeight()
                || e.getX() < Constants.RUNE_MENU_X * (getClientWidth()) ||
                e.getX() > (Constants.RUNE_MENU_X + Constants.RUNE_ITEM_WIDTH) * (getClientWidth())) {
            runePageIndex = -1;
        }
        else {
            runePageIndex = (int) (((e.getY() + scroll) -
                    ((Constants.RUNE_MENU_Y - Math.min(
                            Constants.RUNE_ITEM_HEIGHT * 10,
                            Constants.RUNE_ITEM_HEIGHT * pages.size())) * getHeight())) /
                    (Constants.RUNE_ITEM_HEIGHT * getHeight()));
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        if (runePageIndex != selectedRunePageIndex) {
            selectedRunePageIndex = runePageIndex;
            repaintNow();
        }
    }
}
