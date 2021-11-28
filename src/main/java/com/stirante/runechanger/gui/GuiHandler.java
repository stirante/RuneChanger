package com.stirante.runechanger.gui;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.ChampionSelection;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.client.ScreenPointChecker;
import com.stirante.runechanger.gui.overlay.ChampionSuggestions;
import com.stirante.runechanger.gui.overlay.ClientOverlay;
import com.stirante.runechanger.gui.overlay.RuneMenu;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.ChampionBuild;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.util.*;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

public class GuiHandler {
    public static final int MINIMIZED_POSITION = -32000;
    private static final Logger log = LoggerFactory.getLogger(GuiHandler.class);
    public static final String REGISTRY_WINDOW_METRICS = "Control Panel\\Desktop\\WindowMetrics";
    public static final String REGISTRY_APPLIED_DPI = "AppliedDPI";
    private final AtomicBoolean threadRunning = new AtomicBoolean(false);
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);
    private SyncingListWrapper<ChampionBuild> builds = new SyncingListWrapper<>();
    private final ResourceBundle resourceBundle = LangHelper.getLang();
    private final RuneChanger runeChanger;
    private final ReentrantLock lock = new ReentrantLock();
    private JWindow win;
    private ClientOverlay clientOverlay;
    private WinDef.HWND hwnd;
    private TrayIcon trayIcon;
    private SceneType type = SceneType.NONE;
    private List<Champion> suggestedChampions;
    private Consumer<Champion> suggestedChampionSelectedListener;
    private List<Champion> bannedChampions;
    private int screenCheckCounter = 0;
    private double screenScale = 1d;

    public GuiHandler(RuneChanger runeChanger) {
        this.runeChanger = runeChanger;
        init();
    }

    /**
     * Is window open
     *
     * @return is open
     */
    public boolean isWindowOpen() {
        return windowOpen.get();
    }

    /**
     * Closes window
     */
    public void closeWindow() {
        lock.lock();
        log.info("Closing window");
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.OVERLAY_HIDE);
        if (win != null) {
            win.dispose();
            win = null;
        }
        windowOpen.set(false);
        lock.unlock();
    }

    /**
     * Opens window
     */
    public void openWindow() {
        lock.lock();
        log.info("Opening window");
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.OVERLAY_SHOW);
        if (!windowOpen.get()) {
            startWindow();
            windowOpen.set(true);
        }
        lock.unlock();
        startThread();
    }

    /**
     * Gets scene type. Currently, there can be only 3 types, but there will be more in the future
     */
    public SceneType getSceneType() {
        return type;
    }

    /**
     * Sets scene type. Currently, there can be only 3 types, but there will be more in the future
     */
    public void setSceneType(SceneType type) {
        this.type = type;
        if (clientOverlay != null) {
            clientOverlay.setSceneType(type);
        }
        if (type == SceneType.NONE) {
            builds.clear();
            if (clientOverlay != null) {
                clientOverlay.getLayer(RuneMenu.class).setBuilds(builds.getBackingList());
            }
        }
    }

    public void setBuilds(SyncingListWrapper<ChampionBuild> buildList) {
        builds = buildList;
        if (clientOverlay != null) {
            clientOverlay.getLayer(RuneMenu.class).setBuilds(builds.getBackingList());
        }
    }

    public void showInfoMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.INFO);
        }
    }

    public void showErrorMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.ERROR);
        }
    }

    public void showWarningMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.WARNING);
        }
    }

    private void adjustWindowSize(Rectangle rect) {
        rect.height = (int) (rect.height * screenScale);
        rect.width = (int) (rect.width * screenScale);
        rect.x = (int) (rect.x * screenScale);
        rect.y = (int) (rect.y * screenScale);
    }

    /**
     * Actually create and show client overlay
     *
     * @param rect client window bounds
     */
    private void showWindow(Rectangle rect) {
        adjustWindowSize(rect);
        if (win != null) {
            win.dispose();
        }
        win = new JWindow();
        clientOverlay = new ClientOverlay(runeChanger);
        clientOverlay.getLayer(RuneMenu.class).setBuilds(builds.getBackingList());
        clientOverlay.setSceneType(type);
        win.setContentPane(clientOverlay);
        win.setAlwaysOnTop(true);
        win.setAutoRequestFocus(false);
        win.setFocusable(false);
        win.pack();
        win.setSize((int) (rect.width + (Constants.CHAMPION_SUGGESTION_WIDTH * rect.height)), rect.height);
        win.setBackground(new Color(0f, 0f, 0f, 0f));
        clientOverlay.setSize(rect.width, rect.height);
        trackPosition(rect);
        win.setVisible(true);
        win.setOpacity(1f);
        clientOverlay.addMouseMotionListener(clientOverlay);
        clientOverlay.addMouseListener(clientOverlay);
        clientOverlay.addMouseWheelListener(clientOverlay);
    }

    private void trackPosition(Rectangle rect) {
        win.setLocation(rect.x, rect.y);
    }

    /**
     * Starts GUI thread for button
     */
    private void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        try {
            screenScale = 96d /
                    (double) Advapi32Util.registryGetIntValue(HKEY_CURRENT_USER, REGISTRY_WINDOW_METRICS, REGISTRY_APPLIED_DPI);
        } catch (Exception e) {
            screenScale = 1.0d;
        }
        try {
            //Create icon in system tray and right click menu
            SystemTray systemTray = SystemTray.getSystemTray();
            //Create icon in tray
            Image image =
                    ImageIO.read(GuiHandler.class.getResourceAsStream("/images/16.png"));
            //Create tray menu
            PopupMenu trayPopupMenu = new PopupMenu();
            MenuItem action = new MenuItem("RuneChanger v" + Constants.VERSION_STRING);
            action.setEnabled(false);
            trayPopupMenu.add(action);

            MenuItem settings = new MenuItem(resourceBundle.getString("show_gui"));
            settings.addActionListener(e -> Settings.show());
            trayPopupMenu.add(settings);

            MenuItem launchLol = new MenuItem(resourceBundle.getString("launch_lol"));
            String clientPath = SimplePreferences.getStringValue(SimplePreferences.InternalKeys.CLIENT_PATH, "");
            File file = new File(clientPath);
            if (!clientPath.isEmpty() && file.exists() && new File(file.getParentFile(), "Riot Client").exists()) {
                launchLol.addActionListener(e -> {
                    try {
                        File riotPath = new File(file.getParentFile(), "Riot Client");
                        new ProcessBuilder(new File(riotPath, "RiotClientServices.exe").getAbsolutePath(),
                                "--launch-product=league_of_legends",
                                "--launch-patchline=live")
                                .directory(riotPath)
                                .start();
                    } catch (IOException ioException) {
                        log.error("Exception occurred while launching LoL", ioException);
                        showErrorMessage(LangHelper.getLang().getString("failed_launch_lol"));
                    }
                });
            }
            else {
                launchLol.setEnabled(false);
            }
            trayPopupMenu.add(launchLol);

            MenuItem close = new MenuItem(resourceBundle.getString("exit"));
            close.addActionListener(e -> {
                closeWindow();
                System.exit(0);
            });
            trayPopupMenu.add(close);

            trayIcon = new TrayIcon(image, "RuneChanger", trayPopupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Settings.toggle());
            systemTray.add(trayIcon);
        } catch (Exception e) {
            log.error("Exception occurred while initializing gui", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while initializing gui", true);
            System.exit(0);
        }
        EventBus.register(this);
    }

    /**
     * Internal method, that starts the thread, which adjusts overlay to client window
     */
    private void startThread() {
        if (threadRunning.get()) {
            return;
        }
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            threadRunning.set(true);
            while (windowOpen.get()) {
                long start = System.currentTimeMillis();
                //if window is open set it's position or hide if client is not active window
                lock.lock();
                WinDef.HWND top = User32.INSTANCE.GetForegroundWindow();
                WinDef.RECT rect = new WinDef.RECT();
                User32.INSTANCE.GetWindowRect(top, rect);
                boolean isClientWindow = NativeUtils.isLeagueOfLegendsClientWindow(top);
//                if (isClientWindow) {
//                    screenCheckCounter++;
//                    if (screenCheckCounter >= 10) {
//                        screenCheckCounter = 0;
//                        if (getSceneType() == SceneType.CHAMPION_SELECT ||
//                                getSceneType() == SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT) {
//                            if (ScreenPointChecker.testScreenPoint(top, ScreenPointChecker.CHAMPION_SELECTION_RUNE_PAGE_EDIT)) {
//                                setSceneType(SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT);
//                            }
//                            else {
//                                setSceneType(SceneType.CHAMPION_SELECT);
//                            }
//                        }
//                    }
//                }
                if (win != null) {
                    try {
                        //apparently if left is -32000 then window is minimized
                        if (rect.left != MINIMIZED_POSITION && top != null && hwnd != null &&
                                top.getPointer().equals(hwnd.getPointer()) && !win.isVisible()) {
                            win.setVisible(true);
                        }
                        else {
                            //If top window is not named League of Legends, then hide overlay. Makes funny results when opening folder named League of Legends
                            if (isClientWindow && !win.isVisible()) {
                                win.setVisible(true);
                                hwnd = top;
                            }
                            else if (!isClientWindow) {
                                win.setVisible(false);
                            }
                        }
                        Rectangle rect1 = rect.toRectangle();
                        if (rect1 != null) {
                            adjustWindowSize(rect1);
                            trackPosition(rect1);
                        }
                    } catch (Throwable t) {
                        log.error("Exception occurred while updating window", t);
                        AnalyticsUtil.addCrashReport(t, "Exception occurred while updating window", false);
                    }
                }
                else {
                    if (rect.left != MINIMIZED_POSITION && isClientWindow) {
                        showWindow(rect.toRectangle());
                    }
                }
                lock.unlock();
                try {
                    //60FPS master race
                    Thread.sleep(Math.max(1, 16 - (System.currentTimeMillis() - start)));
                } catch (InterruptedException ignored) {
                }
            }
            threadRunning.set(false);
        });
    }

    /**
     * Searches for League of Legends window and if found, creates a window for it
     */
    private void startWindow() {
        //firstly get client window
        final User32 user32 = User32.INSTANCE;
        user32.EnumWindows((hWnd, arg1) -> {
            boolean isClientWindow = NativeUtils.isLeagueOfLegendsClientWindow(hWnd);

            if (!isClientWindow) {
                return true;
            }
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(hWnd, rect);
            if (rect.top == 0 && rect.left == 0) {
                return true;
            }
            //ignore minimized windows
            if (rect.left == MINIMIZED_POSITION) {
                return true;
            }
            hwnd = hWnd;
            showWindow(rect.toRectangle());
            return true;
        }, null);
    }

    @Subscribe(ClientEventListener.ChampionSelectionEvent.NAME)
    public void onChampionSelection(ClientEventListener.ChampionSelectionEvent event) {
        if (event.getEventType() == ClientEventListener.WebSocketEventType.DELETE) {
            setSceneType(SceneType.NONE);
        }
        else if (getSceneType() != SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT) {
            setSceneType(SceneType.CHAMPION_SELECT);
        }
    }


    @Subscribe(ClientEventListener.ClientZoomScaleEvent.NAME)
    public void onClientZoomScale(ClientEventListener.ClientZoomScaleEvent event) {
        //Client window size changed, so we restart the overlay
        closeWindow();
        openWindow();
    }

    @Subscribe(ChampionSelection.ChampionChangedEvent.NAME)
    public void onChampionChange(ChampionSelection.ChampionChangedEvent event) {
        SyncingListWrapper<ChampionBuild> pages = new SyncingListWrapper<>();
        setBuilds(pages);
        if (event.getChampion() != null) {
            log.info("Downloading runes for champion: " + event.getChampion().getName());
            SourceStore.getRunes(
                    GameData.of(
                            event.getChampion(),
                            RuneChanger.getInstance().getChampionSelectionModule().getGameMode()
                    ).addContext(GameData.Context.CHAMPION_SELECT), pages);
        }
        else {
            log.info("Showing local runes");
            SourceStore.getLocalRunes(pages);
        }
    }

}
