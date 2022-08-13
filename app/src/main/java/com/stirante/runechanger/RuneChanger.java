package com.stirante.runechanger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.google.gson.Gson;
import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.runechanger.api.Champions;
import com.stirante.runechanger.api.RuneBook;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.api.overlay.ClientOverlay;
import com.stirante.runechanger.client.*;
import com.stirante.runechanger.gui.GuiHandler;
import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.model.app.Version;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.util.*;
import com.stirante.runechanger.utils.*;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import generated.LolChampSelectChampSelectPlayerSelection;
import generated.LolChampSelectChampSelectSession;
import generated.LolSummonerSummoner;
import ly.count.sdk.java.Countly;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class RuneChanger implements Launcher, RuneChangerApi {

    private static final Logger log = LoggerFactory.getLogger(RuneChanger.class);
    private static RuneChanger instance;
    private static ChampionsImpl champions;
    public String[] programArguments;
    private ClientApi api;
    private GuiHandler gui;

    private ChampionSelection champSelectModule;
    private Runes runesModule;
    private Loot lootModule;
    private Chat chatModule;
    private Matchmaking matchmakingModule;

    private ClientWebSocket socket;
    private ResourceBundle resourceBundle;
    private RuneBook runeBook;

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-debug-mode")) {
            DebugConsts.enableDebugMode();
            log.debug("Runechanger started with debug mode enabled");
        }
        log.debug("Loading prefs");
        SimplePreferences.load();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Ignore bug JDK-8089615
            if (throwable instanceof NullPointerException && throwable.getStackTrace().length > 0 &&
                    throwable.getStackTrace()[0].getMethodName().equals("isSupported")) {
                return;
            }
            log.error("An unhandled exception occurred in thread " + thread.getName(), throwable);
            AnalyticsUtil.addCrashReport(throwable,
                    "An unhandled exception occurred in thread " + thread.getName(), true);
        });
        instance = new RuneChanger();
        instance.programArguments = args;
        log.debug("Checking and creating lockfile");
        checkAndCreateLockfile();
        log.debug("Changing working directory");
        changeWorkingDir();
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.RUN_AS_ADMIN, false) && !isAdmin()) {
            log.info("Not running as admin, elevating...");
            ShellAPI.SHELLEXECUTEINFO execInfo = new ShellAPI.SHELLEXECUTEINFO();
            execInfo.lpDirectory = PathUtils.getWorkingDirectory();
            execInfo.lpParameters =
                    "-cp \"lib/*\" -Djdk.tls.client.protocols=\"TLSv1,TLSv1.1,TLSv1.2\" --add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.scene.layout=ALL-UNNAMED com.stirante.runechanger.RuneChanger";
            execInfo.lpFile = "image\\bin\\javaw.exe";
            execInfo.lpVerb = "runas";
            if (Shell32.INSTANCE.ShellExecuteEx(execInfo)) {
                System.exit(0);
            }
            else {
                log.error("Failed to elevate: " + Kernel32Util.getLastErrorMessage());
            }
        }
        log.debug("Cleaning up logs");
        AsyncTask.EXECUTOR_SERVICE.submit(RuneChanger::cleanupLogs);
        log.debug("Cleaning up update files");
        AsyncTask.EXECUTOR_SERVICE.submit(AutoUpdater::updateCleanup);
        log.debug("Checking os");
        // This flag is only meant for development. It disables whole client communication
        if (!Arrays.asList(args).contains("-skip-os-check")) {
            checkOperatingSystem();
        }
        log.debug("Initializing champions");
        try {
            champions = new ChampionsImpl(instance);
            champions.init();
        } catch (IOException e) {
            log.error("Exception occurred while initializing champions", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while initializing champions", true);
            JOptionPane.showMessageDialog(null, instance.getLang().getString("init_data_error"), Constants.APP_NAME,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (Arrays.asList(args).contains("-nologs")) {
            logger.getAppender("FILE").stop();
            new File(((FileAppender<ILoggingEvent>) logger.getAppender("FILE")).getFile()).delete();
            logger.detachAppender("FILE");
        }
        try {
            AutoStartUtils.checkAutoStartPath();
        } catch (Exception e) {
            log.error("Exception occurred while checking autostart path", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while checking autostart path", false);
        }
        instance.runeBook = RuneBookImpl.loadRuneBook(instance);
        if (!SimplePreferences.getBooleanValue(SimplePreferences.FlagKeys.CREATED_SHORTCUTS, false) ||
                !SimplePreferences.getBooleanValue(SimplePreferences.FlagKeys.UPDATED_LOGO, false)) {
            try {
                ShortcutUtils.createDesktopShortcut();
                ShortcutUtils.createMenuShortcuts();
                SimplePreferences.putBooleanValue(SimplePreferences.FlagKeys.CREATED_SHORTCUTS, true);
                SimplePreferences.putBooleanValue(SimplePreferences.FlagKeys.UPDATED_LOGO, true);
            } catch (Exception e) {
                log.error("Exception occurred while creating shortcuts", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while creating shortcuts", false);
            }
        }
        instance.init();
    }

    public static RuneChanger getInstance() {
        return instance;
    }

    private static boolean isAdmin() {
        return Advapi32Util.accessCheck(new File("C:/Windows"), Advapi32Util.AccessCheckPermission.WRITE);
    }

    public ResourceBundle getLang() {
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle("lang.messages", LangHelper.getLocale());
        }
        return resourceBundle;
    }

    public boolean isTextRTL() {
        return LangHelper.isTextRTL(getLang().getLocale());
    }

    private static void changeWorkingDir() {
        try {
            //If this is true then the jar was most likely started by autostart
            if (!new File(System.getProperty("user.dir")).getAbsolutePath()
                    .equals(PathUtils.getWorkingDirectory())) {
                //if it's not a jar (probably running from IDE)
                if (!PathUtils.getJarLocation().endsWith(".jar")) {
                    return;
                }

                Runtime.getRuntime()
                        .exec(AutoStartUtils.getStartCommand(), null, new File(PathUtils.getWorkingDirectory()));
                log.warn("User directory: " + new File(System.getProperty("user.dir")).getAbsolutePath());
                log.warn("Working directory: " + PathUtils.getWorkingDirectory());
                log.warn("Runechanger was started from a unusual jvm location most likely due to autostart. " +
                        "Restarting client now to fix pathing errors..");
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("Exception occurred while changing current directory", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while changing current directory", false);
        }
    }

    private static void checkOperatingSystem() {
        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            log.error("User is not on a windows machine");
            JOptionPane.showMessageDialog(null, instance.getLang().getString("windows_only"),
                    Constants.APP_NAME,
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    private static void checkAndCreateLockfile() {
        String userHome = PathUtils.getWorkingDirectory();
        File file = new File(userHome, "runechanger.lock");
        try {
            FileChannel fc = FileChannel.open(file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
            FileLock lock = fc.tryLock();
            if (lock == null) {
                log.error("Another instance of runechanger is open. Exiting program now.");
                System.exit(1);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            log.error("Error creating lockfile", e);
            AnalyticsUtil.addCrashReport(e, "Error creating lockfile", true);
            System.exit(1);
        }
    }

    private static void cleanupLogs() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -30);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext(); ) {
                Appender<ILoggingEvent> appender = index.next();
                if (appender instanceof FileAppender) {
                    FileAppender<ILoggingEvent> fa = (FileAppender<ILoggingEvent>) appender;
                    File logFile = new File(PathUtils.getWorkingDirectory(), fa.getFile());
                    //Remove logs older than 30 days
                    if (logFile.getParentFile().exists()) {
                        for (File file : Objects.requireNonNull(logFile.getParentFile().listFiles())) {
                            if (new Date(file.lastModified()).before(c.getTime())) {
                                if (!file.delete()) {
                                    log.error("Failed to remove logs older then 30 days!");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void initModules() {
        champSelectModule = new ChampionSelection(this);
        runesModule = new Runes(this);
        lootModule = new Loot(this);
        chatModule = new Chat(this);
        matchmakingModule = new Matchmaking(this);
    }

    private void resetModules() {
        champSelectModule.reset();
        runesModule.reset();
        lootModule.reset();
        chatModule.reset();
        matchmakingModule.reset();
    }

    private void init() {
        log.info("Starting RuneChanger version " + getVersion() + " (" + Version.INSTANCE.branch + "@" +
                Version.INSTANCE.commitIdAbbrev + " built at " +
                SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Version.INSTANCE.buildTime) + ")");
        AsyncTask.EXECUTOR_SERVICE.submit(() -> {
            try {
                log.debug(new Gson().toJson(Hardware.getAllHardwareInfo()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        if (!Arrays.asList(programArguments).contains("-no-connection")) {
            try {
                // Disabled due to problems with switching between different LoL installations (PBE and release)
//                String clientPath = SimplePreferences.getStringValue(SimplePreferences.InternalKeys.CLIENT_PATH, null);
//                if (clientPath != null && !new File(clientPath).exists()) {
//                    clientPath = null;
//                }
//                api = new ClientApi(clientPath);
                api = new ClientApi();
            } catch (IllegalStateException e) {
                log.error("Exception occurred while creating client api", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while creating client api", true);
                JOptionPane.showMessageDialog(null, getLang()
                        .getString("client_error"), Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        initModules();
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.EMERGENCY_SHORTCUTS, false)) {
            LolHelper.init();
        }
        Settings.initialize();
        SourceStore.init(this);
        gui = new GuiHandler(this);
        if (!Arrays.asList(programArguments).contains("-no-connection")) {
            api.addClientConnectionListener(new ClientConnectionListener() {
                @Override
                public void onClientConnected() {
                    log.info("Client connected");
                    AnalyticsUtil.beginSession();
                    if (!api.getClientPath()
                            .equalsIgnoreCase(SimplePreferences.getStringValue(SimplePreferences.InternalKeys.CLIENT_PATH, null))) {
                        log.info("Saving client path to \"" + api.getClientPath() + "\"");
                        SimplePreferences.putStringValue(SimplePreferences.InternalKeys.CLIENT_PATH, api.getClientPath());
                    }
                    gui.setSceneType(SceneType.NONE);
                    gui.openWindow();
                    AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        if (DebugConsts.MOCK_SESSION) {
                            gui.showWarningMessage("Mocking session");
                            LolSummonerSummoner currentSummoner = champSelectModule.getCurrentSummoner();
                            LolChampSelectChampSelectSession session = new LolChampSelectChampSelectSession();
                            session.myTeam = new ArrayList<>();
                            LolChampSelectChampSelectPlayerSelection e = new LolChampSelectChampSelectPlayerSelection();
                            e.championId = 223;//Tahm kench
                            e.championPickIntent = 223;
                            e.summonerId = currentSummoner.summonerId;
                            session.myTeam.add(e);
                            EventBus.publish(ChampionSelection.CHAMPION_SELECT_SESSION_EVENT,
                                    new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.CREATE, session));
                        }
                    });
                    // Check if session is active after starting RuneChanger, since we might not get event right away
                    try {
                        LolChampSelectChampSelectSession session = champSelectModule.getSession();
                        if (session != null) {
                            EventBus.publish(ChampionSelection.CHAMPION_SELECT_SESSION_EVENT,
                                    new ClientEventListener.DummyClientEvent<>(ClientEventListener.WebSocketEventType.CREATE, session));
                        }
                    } catch (Exception ignored) {
                    }
                    // Auto sync rune pages to RuneChanger
                    if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_SYNC, false)) {
                        runesModule.syncRunePages();
                    }
                    //sometimes, the api is connected too quickly and there is WebsocketNotConnectedException
                    //That's why I added this little piece of code, which will retry opening socket every second
                    AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                        while (true) {
                            try {
                                openSocket();
                                return;
                            } catch (Exception e) {
                                if (!api.isConnected()) {
                                    return;
                                }
                                if (e instanceof WebsocketNotConnectedException || e instanceof ConnectException) {
                                    log.error("Connection failed, retrying in a second..");
                                    //try again in a second
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ignored) {
                                    }
                                    continue;
                                }
                                else {
                                    log.error("Exception occurred while opening socket", e);
                                    AnalyticsUtil.addCrashReport(e, "Exception occurred while opening socket", false);
                                }
                            }
                            return;
                        }
                    });
                }

                @Override
                public void onClientDisconnected() {
                    if (Countly.isInitialized()) {
                        Countly.session().end();
                    }
                    if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_EXIT, false)) {
                        System.exit(0);
                    }
                    else {
                        resetModules();
                        gui.setSceneType(SceneType.NONE);
                        if (gui.isWindowOpen()) {
                            gui.closeWindow();
                        }
                        gui.showInfoMessage(getLang().getString("client_disconnected"));
                        Settings.setClientConnected(false);
                    }
                }
            });
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LolHelper.stop();
            if (socket != null) {
                socket.close();
            }
            log.info("Socket closed");
            if (Countly.isInitialized() && Countly.session().isActive()) {
                Countly.session().end();
            }
            log.info("Countly closed");
            Countly.stop(false);
            log.info("Countly stopped");
            PerformanceMonitor.stop();
            log.info("Perfmon stopped");
        }));
        AnalyticsUtil.init(api.isConnected());
        EventBus.register(this);
    }

    @Subscribe(ClientEventListener.SOCKET_CLOSE_EVENT)
    public void onSocketClose() {
        socket = null;
    }

    private void openSocket() throws Exception {
        socket = api.openWebSocket();
        gui.showInfoMessage(getLang().getString("client_connected"));
        Settings.setClientConnected(true);
        socket.setSocketListener(new ClientEventListener());
    }

    public ChampionSelection getChampionSelectionModule() {
        return champSelectModule;
    }

    public Runes getRunesModule() {
        return runesModule;
    }

    public Loot getLootModule() {
        return lootModule;
    }

    public Chat getChatModule() {
        return chatModule;
    }

    public Matchmaking getMatchmakingModule() {
        return matchmakingModule;
    }

    @Override
    public void run(LaunchContext context) {
        try {
            AutoUpdater.deleteOldLibs();
            Runtime.getRuntime().exec("wscript silent.vbs open.bat");
            System.exit(0);
        } catch (IOException e) {
            log.error("Exception occurred while executing command", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while executing command", false);
        }
    }

    public GuiHandler getGuiHandler() {
        return gui;
    }

    @Override
    public RuneBook getRuneBook() {
        return runeBook;
    }

    @Override
    public String getVersion() {
        return Version.INSTANCE.version;
    }

    @Override
    public ClientOverlay getClientOverlay() {
        return getGuiHandler().getClientOverlay();
    }

    @Override
    public ClientApi getClientApi() {
        return api;
    }

    @Override
    public Champions getChampions() {
        return champions;
    }
}
