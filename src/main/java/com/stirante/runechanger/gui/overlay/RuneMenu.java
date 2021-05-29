package com.stirante.runechanger.gui.overlay;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.ChampionSelection;
import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.SwingUtils;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RuneMenu extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(RuneMenu.class);
    private List<RunePage> pages = new ArrayList<>();
    private boolean opened = false;
    private int selectedRunePageIndex = -1;
    private BufferedImage icon;
    private BufferedImage grayscaleIcon;
    private Image warnIcon;
    private Image closeIcon;
    private final Rectangle warningCloseButton = new Rectangle(0, 0, 0, 0);
    private boolean warningVisible = true;
    private float currentRuneMenuPosition = 0f;
    private float scroll = 0f;
    private int size = 0;

    public RuneMenu(ClientOverlay overlay) {
        super(overlay);
        try {
            warnIcon =
                    SwingUtils.getScaledImage(32, 32, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/info-yellow.png"))));
            closeIcon =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/close.png"))));
        } catch (IOException e) {
            log.error("Exception occurred while loading rune button icons", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading rune button icons", false);
        }
        EventBus.register(this);
    }

    @Subscribe(ChampionSelection.ChampionSelectionEndEvent.NAME)
    public void onChampionSelectionEnd(ChampionSelection.ChampionSelectionEndEvent e) {
        warningVisible = false;
    }

    @Override
    protected void draw(Graphics g) {
//        drawWarning((Graphics2D) g);
        if (getSceneType() == SceneType.CHAMPION_SELECT) {
            drawRuneButton((Graphics2D) g);
            drawRuneMenu((Graphics2D) g);
        }
    }

    public void setRuneData(ObservableList<RunePage> pages) {
        this.pages = pages;
        repaintNow();
        pages.addListener((InvalidationListener) observable -> FxUtils.doOnFxThread(this::repaintNow));
    }

    private void drawRuneButton(Graphics2D g2d) {
        int size = (int) (Constants.RUNE_BUTTON_SIZE * getHeight());
        if (size != this.size || icon == null || grayscaleIcon == null) {
            this.size = size;
            try {
                icon =
                        SwingUtils.getScaledImage(size, size, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/28.png"))));
                grayscaleIcon =
                        SwingUtils.getScaledImage(size, size, ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/28grayscale.png"))));
            } catch (Exception e) {
                log.error("Exception occurred while loading rune button icons", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while loading rune button icons", false);
                return;
            }
        }
        int x = (int) ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X);
        int y = (int) (getHeight() * Constants.RUNE_BUTTON_POSITION_Y);
        if (pages.size() > 0) {
            g2d.drawImage(icon, x, y, icon.getWidth(), icon.getHeight(), null);
        }
        else {
            g2d.drawImage(grayscaleIcon, x, y, grayscaleIcon.getWidth(), grayscaleIcon.getHeight(), null);
            opened = false;
        }
    }

    private void drawWarning(Graphics2D g2d) {
        if (!warningVisible) {
            return;
        }
        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(15f));

        String text = "Did you forget to change the rune page?";
        LineMetrics metrics = g2d.getFontMetrics().getLineMetrics(text, g2d);
        int border = 2;
        int margin = 5;
        int height = 50;
        int width = (2 * border) + (4 * margin) + warnIcon.getWidth(null) + g2d.getFontMetrics().stringWidth(text) +
                closeIcon.getWidth(null);
        int x = (int) ((getClientWidth() * Constants.WARNING_X) - (width / 2));
        int y = (int) (getHeight() * Constants.WARNING_Y);

        g2d.setPaint(new GradientPaint(0, y, GRADIENT_COLOR_1, 0, y + height, GRADIENT_COLOR_2));
        g2d.fillRect(x, y, width, height);
        g2d.setPaint(BACKGROUND_COLOR);
        g2d.fillRect(x + border, y + border, width - (2 * border), height - (2 * border));
        g2d.setPaint(TEXT_COLOR);
        g2d.drawImage(
                closeIcon,
                (x + width - (border * 2)) - closeIcon.getWidth(null),
                y + border,
                null
        );
        warningCloseButton.setLocation((x + width - (border * 2)) - closeIcon.getWidth(null), y + border);
        warningCloseButton.setSize(closeIcon.getWidth(null), closeIcon.getHeight(null));
        g2d.drawImage(
                warnIcon,
                x + border + margin,
                y + (height / 2) - (warnIcon.getHeight(null) / 2),
                null
        );
        g2d.drawString(
                text,
                x + border + (margin * 2) + warnIcon.getWidth(null),
                y + (height / 2) + (int) (metrics.getAscent() / 4)
        );

        g2d.setFont(oldFont);
    }

    private void drawRuneMenu(Graphics2D g2d) {
        //open animations
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
            currentRuneMenuPosition = 0f;
            scroll = 0f;
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
        if (icon == null) {
            return;
        }
        if (warningVisible && warningCloseButton.getSize().height > 0 &&
                warningCloseButton.contains(e.getX(), e.getY())) {
            warningVisible = false;
        }
        else if (pages.size() > 0 && e.getX() > ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) &&
                e.getX() < ((getClientWidth()) * Constants.RUNE_BUTTON_POSITION_X) + icon.getWidth() &&
                e.getY() > (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) &&
                e.getY() < (getHeight() * Constants.RUNE_BUTTON_POSITION_Y) + icon.getHeight()) {
            opened = !opened;
        }
        else if (selectedRunePageIndex > -1) {
            RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                if (pages.size() > selectedRunePageIndex) {
                    RuneChanger.getInstance()
                            .getRunesModule()
                            .setCurrentRunePage(pages.get(selectedRunePageIndex));
                }
            });
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
        if (icon == null) {
            return;
        }
        int runePageIndex;
        if (warningVisible && warningCloseButton.getSize().height > 0 &&
                warningCloseButton.contains(e.getX(), e.getY())) {
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
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
            // Prevent getting nonexistent rune page
            selectedRunePageIndex = runePageIndex >= pages.size() || runePageIndex < 0 ? -1 : runePageIndex;
            repaintNow();
        }
    }
}
