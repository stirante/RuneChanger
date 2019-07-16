package com.stirante.RuneChanger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.stirante.RuneChanger.client.ChampionSelection;
import com.stirante.RuneChanger.client.Runes;
import com.stirante.RuneChanger.gui.*;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.model.github.Version;
import com.stirante.RuneChanger.runestore.RuneStore;
import com.stirante.RuneChanger.util.*;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import generated.*;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class RuneChanger {

    public String[] programArguments;
    private static RuneChanger instance;
    private ClientApi api;
    private GuiHandler gui;
    private List<RunePage> runes;
    private ChampionSelection champSelectModule;
    private Runes runesModule;
    private ClientWebSocket socket;

    public static void main(String[] args) {
        changeWorkingDir();
        cleanupLogs();
        setDefaultUncaughtExceptionHandler();
        Elevate.elevate(args);
        checkOperatingSystem();
        SimplePreferences.load();
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (Arrays.asList(args).contains("-debug-mode")) {
            logger.setLevel(Level.DEBUG);
            logger.debug("Runechanger started with debug mode enabled");
        }
        if (Arrays.asList(args).contains("-nologs")) {
            logger.detachAppender("FILE");
            logger.warn("warning");
        }
        try {
            AutoUpdater.cleanup();
            AutoStartUtils.checkAutoStartPath();
            if (!AutoUpdater.check()) {
                JFrame frame = new JFrame();
                frame.setUndecorated(true);
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
                int dialogResult =
                        JOptionPane.showConfirmDialog(frame, LangHelper.getLang()
                                .getString("update_question"), LangHelper.getLang()
                                .getString("update_available"), JOptionPane.YES_NO_OPTION);
                frame.dispose();
                if (dialogResult == JOptionPane.YES_OPTION) {
                    AutoUpdater.performUpdate();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Champion.init();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, LangHelper.getLang().getString("init_data_error"), Constants.APP_NAME,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        instance = new RuneChanger();
        instance.programArguments = args;
        instance.init();
    }

    public static RuneChanger getInstance() {
        return instance;
    }

    private static void changeWorkingDir() {
        try {
            //find path to the current jar
            File currentJar = new File(PathUtils.getJarLocation());
            //If this is true then the jar was most likely started by autostart
            if (!new File(System.getProperty("user.dir")).getAbsolutePath().equals(currentJar.getParentFile().getAbsolutePath())) {
                //if it's not a jar (probably running from IDE)
                if (!currentJar.getName().endsWith(".jar")) {
                    return;
                }

                //construct command and run it
                final ArrayList<String> command = new ArrayList<>();
                command.add(PathUtils.getJavawPath());
                command.add("-jar");
                command.add(currentJar.getPath());
                command.add("-minimized");

                final ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(currentJar.getParentFile());
                builder.start();
                log.warn("Runechanger was started from a unusual jvm location most likely due to autostart. " +
                        "Restarting client now to fix pathing errors..");
                log.info("Restart command: " + command.toString());
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkOperatingSystem() {
        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            log.error("User is not on a windows machine");
            JOptionPane.showMessageDialog(null, LangHelper.getLang().getString("windows_only"),
                    Constants.APP_NAME,
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    private void init() {
        log.info("Starting RuneChanger version " + Constants.VERSION_STRING + " (" + Version.INSTANCE.branch + "@" +
                Version.INSTANCE.commitIdAbbrev + " built at " +
                SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Version.INSTANCE.buildTime) + ")");
        ClientApi.setDisableEndpointWarnings(true);
        try {
            api = new ClientApi();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, LangHelper.getLang()
                    .getString("client_error"), Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        champSelectModule = new ChampionSelection(api);
        runesModule = new Runes(api);
        Settings.initialize();
        gui = new GuiHandler(this);
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                gui.setSceneType(SceneType.NONE);
                gui.openWindow();
                if (DebugConsts.MOCK_SESSION) {
                    gui.showWarningMessage("Mocking session");
                    try {
                        LolSummonerSummoner currentSummoner =
                                api.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class);
                        LolChampSelectChampSelectSession session = new LolChampSelectChampSelectSession();
                        session.myTeam = new ArrayList<>();
                        LolChampSelectChampSelectPlayerSelection e = new LolChampSelectChampSelectPlayerSelection();
                        e.championId = Objects.requireNonNull(Champion.getByName("tahm kench")).getId();
                        e.summonerId = currentSummoner.summonerId;
                        session.myTeam.add(e);
                        handleSession(session);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //sometimes, the api is connected too quickly and there is WebsocketNotConnectedException
                //That's why I added this little piece of code, which will retry opening socket every second
                new Thread(() -> {
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
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                                continue;
                            }
                            else {
                                e.printStackTrace();
                            }
                        }
                        return;
                    }
                }).run();
            }

            @Override
            public void onClientDisconnected() {
                gui.setSceneType(SceneType.NONE);
                if (gui.isWindowOpen()) {
                    gui.tryClose();
                }
                gui.showInfoMessage(LangHelper.getLang().getString("client_disconnected"));
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (socket != null) {
                socket.close();
            }
        }));
    }

    private void onChampionChanged(Champion champion) {
        log.info("Downloading runes for champion: " + champion.getName());
        runes = RuneStore.getRunes(champion);
        if (runes == null || runes.isEmpty()) {
            log.warn("Runes for champion not available");
        }
        log.info("Found runes: " + runes);
        //remove all invalid rune pages
        runes.removeIf(rune -> !rune.verify());
        if (runes.isEmpty()) {
            log.error("Found error in rune source");
        }
        log.info("Runes available. Showing button");
        gui.setRunes(runes, (page) -> {
            if (runes == null || runes.isEmpty()) {
                return;
            }
            new Thread(() -> runesModule.setCurrentRunePage(page)).start();
        });
    }

    private void handleSession(LolChampSelectChampSelectSession session) {
        Champion oldChampion = champSelectModule.getSelectedChampion();
        champSelectModule.onSession(session);
        if (gui.getSceneType() == SceneType.NONE) {
            gui.setSuggestedChampions(champSelectModule.getLastChampions(), champSelectModule.getBannedChampions(),
                    champSelectModule::selectChampion);
        }
        gui.setSceneType(SceneType.CHAMPION_SELECT);
        if (champSelectModule.getSelectedChampion() != null &&
                champSelectModule.getSelectedChampion() != oldChampion) {
            onChampionChanged(champSelectModule.getSelectedChampion());
        }
    }

    private void openSocket() throws Exception {
        socket = api.openWebSocket();
        gui.showInfoMessage(LangHelper.getLang().getString("client_connected"));
        socket.setSocketListener(new ClientWebSocket.SocketListener() {
            @Override
            public void onEvent(ClientWebSocket.Event event) {
                //printing every event except voice for experimenting
                if (DebugConsts.PRINT_EVENTS && !event.getUri().toLowerCase().contains("voice")) {
                    log.info("Event: " + event);
                }
                if (event.getUri().equalsIgnoreCase("/lol-chat/v1/me") &&
                        SimplePreferences.containsKey("antiAway") &&
                        SimplePreferences.getValue("antiAway").equalsIgnoreCase("true")) {
                    if (((LolChatUserResource) event.getData()).availability.equalsIgnoreCase("away")) {
                        LolChatUserResource data = (LolChatUserResource) event.getData();
                        data.availability = "chat";
                        new Thread(() -> {
                            try {
                                api.executePut("/lol-chat/v1/me", data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).run();
                    }
                }
                if (event.getUri().equalsIgnoreCase("/lol-champ-select/v1/session")) {
                    if (event.getEventType().equalsIgnoreCase("Delete")) {
                        gui.setSceneType(SceneType.NONE);
                        champSelectModule.clearSession();
                    }
                    else {
                        handleSession((LolChampSelectChampSelectSession) event.getData());
                    }
                }
                else if (Boolean.parseBoolean(SimplePreferences.getValue("autoAccept")) &&
                        event.getUri().equalsIgnoreCase("/lol-lobby/v2/lobby/matchmaking/search-state")) {
                    if (((LolLobbyLobbyMatchmakingSearchResource) event.getData()).searchState ==
                            LolLobbyLobbyMatchmakingSearchState.FOUND) {
                        try {
                            api.executePost("/lol-matchmaking/v1/ready-check/accept");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if (event.getUri().equalsIgnoreCase("/riotclient/zoom-scale")) {
                    //Client window size changed, so we restart the overlay
                    gui.tryClose();
                    gui.openWindow();
                }
            }

            @Override
            public void onClose(int i, String s) {
                socket = null;
            }
        });
    }

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error(
                    "Uncaught Exception detected in thread " + t, e));
        } catch (SecurityException e) {
            log.error("Could not set the Default Uncaught Exception Handler", e);
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
                                file.delete();
                            }
                        }
                    }
                }
            }
        }
    }

    public ClientApi getApi() {
        return api;
    }

    public ChampionSelection getChampionSelectionModule() {
        return champSelectModule;
    }

    public Runes getRunesModule() {
        return runesModule;
    }
}
