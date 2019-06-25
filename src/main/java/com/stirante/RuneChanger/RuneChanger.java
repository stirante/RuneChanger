package com.stirante.RuneChanger;

import com.stirante.RuneChanger.client.ChampionSelection;
import com.stirante.RuneChanger.client.Runes;
import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.gui.GuiHandler;
import com.stirante.RuneChanger.gui.SceneType;
import com.stirante.RuneChanger.gui.Settings;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.runestore.RuneStore;
import com.stirante.RuneChanger.util.AutoUpdater;
import com.stirante.RuneChanger.util.Elevate;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.SimplePreferences;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import generated.*;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import javax.swing.*;
import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RuneChanger {

    private static RuneChanger instance;
    private ClientApi api;
    private GuiHandler gui;
    private List<RunePage> runes;
    private ChampionSelection champSelectModule;
    private Runes runesModule;
    private ClientWebSocket socket;

    private void init() {
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
                                d("Connection failed, retrying in a second...");
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
        d("Player chose champion " + champion.getName());
        d("Downloading runes");
        runes = RuneStore.getRunes(champion);
        if (runes == null || runes.isEmpty()) {
            d("Runes for champion not available");
        }
        d(runes);
        //remove all invalid rune pages
        runes.removeIf(rune -> !rune.verify());
        if (runes.isEmpty()) {
            d("Found error in rune source");
        }
        d("Runes available. Showing button");
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

    public static void main(String[] args) {
        Elevate.elevate(args);
        checkOperatingSystem();
        SimplePreferences.load();
        try {
            AutoUpdater.cleanup();
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
        instance.init();
    }

    private void openSocket() throws Exception {
        socket = api.openWebSocket();
        gui.showInfoMessage(LangHelper.getLang().getString("client_connected"));
        socket.setSocketListener(new ClientWebSocket.SocketListener() {
            @Override
            public void onEvent(ClientWebSocket.Event event) {
                //printing every event except voice for experimenting
                if (DebugConsts.PRINT_EVENTS && !event.getUri().toLowerCase().contains("voice")) {
                    System.out.println(event);
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

    /**
     * Debug message
     *
     * @param message message
     */
    public static void d(Object message) {
        System.out.println("[" + SimpleDateFormat.getTimeInstance().format(new Date()) + "] " +
                (message != null ? message.toString() : "null"));
    }

    public static RuneChanger getInstance() {
        return instance;
    }

    public ClientApi getApi() {
        return api;
    }

    private static void checkOperatingSystem() {
        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            d("User is not on a windows machine.");
            JOptionPane.showMessageDialog(null, LangHelper.getLang().getString("windows_only"),
                    Constants.APP_NAME,
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    public ChampionSelection getChampionSelectionModule() {
        return champSelectModule;
    }

    public Runes getRunesModule() {
        return runesModule;
    }
}
