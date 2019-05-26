package com.stirante.RuneChanger;

import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.gui.GuiHandler;
import com.stirante.RuneChanger.gui.Settings;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.runestore.RuneStore;
import com.stirante.RuneChanger.util.Elevate;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.SimplePreferences;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import generated.*;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RuneChanger {

    //Used for testing the UI
    private static final boolean MOCK_SESSION = false;
    private static ClientApi api;
    private static GuiHandler gui;
    private static List<RunePage> runes;
    private static Champion champion;
    private static LolSummonerSummoner currentSummoner;
    private static ClientWebSocket socket;

    private static void handleSession(LolChampSelectChampSelectSession session) {
        gui.openWindow();
        Champion oldChampion = champion;
        champion = null;
        try {
            if (currentSummoner == null) {
                currentSummoner = api.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class);
            }
            //find selected champion
            for (LolChampSelectChampSelectPlayerSelection selection : session.myTeam) {
                if (Objects.equals(selection.summonerId, currentSummoner.summonerId)) {
                    //first check locked champion
                    champion = Champion.getById(selection.championId);
                    //if it fails check just selected champion
                    if (champion == null) {
                        champion = Champion.getById(selection.championPickIntent);
                    }
                    //if all fails check list of actions
                    if (champion == null) {
                        for (Object actionList : session.actions) {
                            for (Object obj : ((List) actionList)) {
                                //noinspection unchecked
                                Map<String, Object> selectAction = (Map<String, Object>) obj;
                                if (selectAction.get("type").equals("pick") &&
                                        selectAction.get("actorCellId") == selection.cellId) {
                                    champion = Champion.getById((Integer) selectAction.get("championId"));
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (champion == null && gui.isWindowOpen()) {
                gui.tryClose();
            }
            if (champion != null && champion != oldChampion) {
                d("Player chose champion " + champion.getName());
                //get all rune pages
                LolPerksPerkPageResource[] pages =
                        api.executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
                //find available pages
                ArrayList<LolPerksPerkPageResource> availablePages = new ArrayList<>();
                for (LolPerksPerkPageResource p : pages) {
                    if (p.isEditable) {
                        availablePages.add(p);
                    }
                }
                if (availablePages.isEmpty()) {
                    d("No editable pages! (That's weird)");
                    return;
                }
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
                    if (champion == null || runes == null || runes.isEmpty()) {
                        return;
                    }
                    new Thread(() -> {
                        try {
                            //change pages
                            LolPerksPerkPageResource page1 =
                                    api.executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class);
                            if (!page1.isEditable || !page1.isActive) {
                                page1 = availablePages.get(0);
                            }
                            page.toClient(page1);
                            api.executeDelete("/lol-perks/v1/pages/" + page1.id);
                            api.executePost("/lol-perks/v1/pages/", page1);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                });
            }
        } catch (FileNotFoundException ignored) {
            //when api returns 404
            gui.tryClose();
        } catch (ConnectException e) {
            //can't connect to client api, probably client is closed
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, LangHelper.getLang().getString("client_off"), Constants.APP_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } catch (SSLHandshakeException e) {
            d("SSLHandshakeException: Nothing too serious unless this message spams whole console");
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            gui.tryClose();
        }
    }

    public static void main(String[] args) {
        Elevate.elevate(args);
        checkOperatingSystem();
        SimplePreferences.load();
        try {
            Champion.init();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, LangHelper.getLang().getString("init_data_error"), Constants.APP_NAME,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        try {
            api = new ClientApi();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, LangHelper.getLang()
                    .getString("client_error"), Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        Settings.initialize();
        gui = new GuiHandler();
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    if (MOCK_SESSION) {
                        gui.showWarningMessage("Mocking session");
                        try {
                            currentSummoner =
                                    api.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class);
                            LolChampSelectChampSelectSession session = new LolChampSelectChampSelectSession();
                            session.myTeam = new ArrayList<>();
                            LolChampSelectChampSelectPlayerSelection e = new LolChampSelectChampSelectPlayerSelection();
                            e.championId = Objects.requireNonNull(Champion.getByName("darius")).getId();
                            e.summonerId = currentSummoner.summonerId;
                            session.myTeam.add(e);
                            handleSession(session);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    socket = api.openWebSocket();
                    gui.showInfoMessage(LangHelper.getLang().getString("client_connected"));
                    socket.setSocketListener(new ClientWebSocket.SocketListener() {
                        @Override
                        public void onEvent(ClientWebSocket.Event event) {
                            //printing every event except voice for experimenting
                            //if (!event.getUri().toLowerCase().contains("voice")) System.out.println(event);
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
                                    gui.tryClose();
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
                        }

                        @Override
                        public void onClose(int i, String s) {
                            socket = null;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {
                gui.showInfoMessage(LangHelper.getLang().getString("client_disconnected"));
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (socket != null) {
                socket.close();
            }
        }));
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

    public static ClientApi getApi() {
        return api;
    }

    public static void sendMessageToChampSelect(String msg) {
        new Thread(() -> {
            try {
                LolChampSelectChampSelectSession session = RuneChanger.getApi()
                        .executeGet("/lol-champ-select/v1/session", LolChampSelectChampSelectSession.class);
                String name = session.chatDetails.chatRoomName;
                if (name == null) {
                    return;
                }
                name = name.substring(0, name.indexOf('@'));
                LolChatConversationMessageResource message = new LolChatConversationMessageResource();
                message.body = msg;
                message.type = "chat";
                try {
                    RuneChanger.getApi().executePost("/lol-chat/v1/conversations/" + name + "/messages", message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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

}
