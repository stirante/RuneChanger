package com.stirante.RuneChanger;

import com.stirante.RuneChanger.crawler.RuneCrawler;
import com.stirante.RuneChanger.gui.GuiHandler;
import com.stirante.RuneChanger.gui.Settings;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.Rune;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.util.Elevate;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.SimplePreferences;
import com.stirante.lolclient.ClientApi;
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

public class InGameButton {

    //Used for testing the UI
    private static final boolean MOCK_SESSION = false;

    private static ClientApi api;
    private static GuiHandler gui;
    private static List<RunePage> runes;
    private static Champion champion;
    private static ResourceBundle resourceBundle = LangHelper.getLang();
    private static LolSummonerSummoner currentSummoner;
    private static ClientWebSocket socket;

    private static void handleSession(LolChampSelectChampSelectSession session) {
        Champion oldChampion = champion;
        champion = null;
        try {
            if (currentSummoner == null)
                currentSummoner = api.getCurrentSummoner();
            //find selected champion
            for (LolChampSelectChampSelectPlayerSelection selection : session.myTeam) {
                if (Objects.equals(selection.summonerId, currentSummoner.summonerId)) {
                    //first check locked champion
                    champion = Champion.getById(selection.championId);
                    //if it fails check just selected champion
                    if (champion == null) champion = Champion.getById(selection.championPickIntent);
                    //if all fails check list of actions
                    if (champion == null) {
                        for (Object actionList : session.actions) {
                            for (Object obj : ((List) actionList)) {
                                Map<String, Object> selectAction = (Map<String, Object>) obj;
                                if (selectAction.get("type").equals("pick") && selectAction.get("actorCellId") == selection.cellId) {
                                    champion = Champion.getById((Integer) selectAction.get("championId"));
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (champion == null && gui.isWindowOpen()) gui.tryClose();
            if (champion != null && champion != oldChampion) {
                d("Player chose champion " + champion.getName());
                //get all rune pages
                LolPerksPerkPageResource[] pages = api.executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
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
                runes = RuneCrawler.getRunes(champion);
                if (runes == null || runes.isEmpty()) d("Runes for champion not available");
                d(runes);
                //remove all invalid rune pages
                runes.removeIf(rune -> !rune.verify());
                if (runes.isEmpty()) {
                    d("Found error in rune source");
                }
                d("Runes available. Showing button");
                gui.openWindow(runes, (page) -> {
                    if (champion == null || runes == null || runes.isEmpty()) return;
                    //sometimes champion changes async and causes errors
                    final Champion finalChamp = champion;
                    new Thread(() -> {
                        try {
                            //change pages
                            LolPerksPerkPageResource page1 = availablePages.get(0);
                            page1.primaryStyleId = page.getMainStyle().getId();
                            page1.subStyleId = page.getSubStyle().getId();
                            page1.name = (finalChamp.getName() + ":" + page.getName());
                            //limit name to 25 characters (client limit)
                            page1.name = page1.name.substring(0, Math.min(25, page1.name.length()));
                            page1.selectedPerkIds.clear();
                            for (Rune rune : page.getRunes()) {
                                page1.selectedPerkIds.add(rune.getId());
                            }
                            page1.isActive = true;
                            api.executePut("/lol-perks/v1/pages/" + page1.id, page1);
                            api.executePut("/lol-perks/v1/currentpage", page1.id);
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
            JOptionPane.showMessageDialog(null, resourceBundle.getString("clientOff"), "RuneChanger", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } catch (SSLHandshakeException e) {
            d("SSLHandshakeException: Nothing too serious unless this message spams whole console");
        } catch (SocketTimeoutException e) {
//            d("SocketTimeoutException: Nothing too serious unless this message spams whole console");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            gui.tryClose();
        }
    }

    public static void main(String[] args) {
        Elevate.elevate(args);
        SimplePreferences.load();
        try {
            api = new ClientApi();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, resourceBundle.getString("noClient"), "RuneChanger", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
        Settings.initialize();
        gui = new GuiHandler();
        if (MOCK_SESSION) {
            try {
                currentSummoner = api.getCurrentSummoner();
                LolChampSelectChampSelectSession session = new LolChampSelectChampSelectSession();
                session.myTeam = new ArrayList<>();
                LolChampSelectChampSelectPlayerSelection e = new LolChampSelectChampSelectPlayerSelection();
                e.championId = Champion.DARIUS.getId();
                e.summonerId = currentSummoner.summonerId;
                session.myTeam.add(e);
                handleSession(session);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            socket = api.openWebSocket();
            socket.setSocketListener(new ClientWebSocket.SocketListener() {
                @Override
                public void onEvent(ClientWebSocket.Event event) {
                    if (event.getUri().equalsIgnoreCase("/lol-champ-select/v1/session")) {
                        if (event.getEventType().equalsIgnoreCase("Delete")) gui.tryClose();
                        else handleSession((LolChampSelectChampSelectSession) event.getData());
                    } else if (SimplePreferences.getValue("autoAccept").equalsIgnoreCase("true") && event.getUri().equalsIgnoreCase("/lol-lobby/v2/lobby/matchmaking/search-state")) {
                        if (((LolLobbyLobbyMatchmakingSearchResource) event.getData()).searchState == LolLobbyLobbyMatchmakingSearchState.FOUND) {
                            try {
                                api.executePost("/lol-matchmaking/v1/ready-check/accept");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (event.getUri().equalsIgnoreCase("/riotclient/ux-state/request") && ((String) ((Map<String, Object>) event.getData()).get("state")).equalsIgnoreCase("Quit")) {
                        socket.close();
                    }
                }

                @Override
                public void onClose(int i, String s) {
                    socket = null;
                    try {
                        JOptionPane.showMessageDialog(null, resourceBundle.getString("clientOff"), "RuneChanger", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                    } catch (ExceptionInInitializerError ignored) {

                    }
                }
            });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (socket != null) socket.close();
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Debug message
     *
     * @param message message
     */
    public static void d(Object message) {
        System.out.println("[" + SimpleDateFormat.getTimeInstance().format(new Date()) + "] " + (message != null ? message.toString() : "null"));
    }

    public static ClientApi getApi() {
        return api;
    }

}
