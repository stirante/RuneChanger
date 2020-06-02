package com.stirante.runechanger.client;

import com.stirante.eventbus.BusEvent;
import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.GameMap;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.util.AnalyticsUtil;
import generated.*;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChampionSelection extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(ChampionSelection.class);

    private Map<String, Object> action;
    private Champion champion;
    private GameMode gameMode;
    private boolean positionSelector;
    private ArrayList<Champion> banned = new ArrayList<>();
    private GameMap map;
    private String currentPhase = "";
    private long phaseEnd = 0L;

    public ChampionSelection(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    public ArrayList<Champion> getLastChampions() {
        try {
            ArrayList<Champion> lastChampions = new ArrayList<>();
            LolMatchHistoryMatchHistoryList historyList =
                    getApi().executeGet("/lol-match-history/v2/matchlist",
                            LolMatchHistoryMatchHistoryList.class,
                            "begIndex", "0",
                            "endIndex", "20");
            if (historyList == null || historyList.games == null) {
                return null;
            }
            List<LolMatchHistoryMatchHistoryGame> games = historyList.games.games;
            historyList =
                    getApi().executeGet("/lol-match-history/v2/matchlist",
                            LolMatchHistoryMatchHistoryList.class,
                            "begIndex", "20",
                            "endIndex", "40");
            games.addAll(historyList.games.games);
            games.sort((o1, o2) -> o2.gameCreation.compareTo(o1.gameCreation));
            for (LolMatchHistoryMatchHistoryGame game : games) {
                LolMatchHistoryMatchHistoryParticipant p = game.participants.stream()
                        .findFirst()
                        .orElse(null);
                if (p == null) {
                    continue;
                }
                if (!lastChampions.contains(Champion.getById(p.championId))) {
                    lastChampions.add(Champion.getById(p.championId));
                }
            }
            return new ArrayList<>(lastChampions);
        } catch (Exception e) {
            log.error("Exception occurred while getting last picked champions", e);
            return null;
        }
    }

    public Champion getSelectedChampion() {
        return champion;
    }

    public ArrayList<Champion> getBannedChampions() {
        return banned;
    }

    @SuppressWarnings("unchecked")
    private void findCurrentAction(LolChampSelectChampSelectSession session) {
        banned.clear();
        //we need to find summoner's cell id
        LolChampSelectChampSelectPlayerSelection self =
                session.myTeam.stream()
                        .filter(player -> player.summonerId.equals(getCurrentSummoner().summonerId))
                        .findFirst()
                        .orElse(null);
        //should never be null, but i'll check just in case
        if (!DebugConsts.MOCK_SESSION && self != null) {
            for (Object actions : session.actions) {
                for (Object action : ((List) actions)) {
                    Map<String, Object> a = (Map<String, Object>) action;
                    //no idea why, but cell id gets recognized here as Double
                    if (((Double) a.get("actorCellId")).intValue() == self.cellId.intValue() &&
                            a.get("type").equals("pick")) {
                        this.action = a;
                    }
                    if (a.get("type").equals("ban") && ((Boolean) a.get("completed"))) {
                        int championId = ((Double) a.get("championId")).intValue();
                        if (championId != 0) {
                            banned.add(Champion.getById(championId));
                        }
                    }
                }
            }
        }
        else if (DebugConsts.MOCK_SESSION) {
            //mock some banned champions
            banned.add(Champion.getByName("blitzcrank"));
            banned.add(Champion.getByName("morgana"));
            banned.add(Champion.getByName("kayle"));
            banned.add(Champion.getByName("leona"));
        }
    }

    private void findSelectedChampion(LolChampSelectChampSelectSession session) {
        //find selected champion
        for (LolChampSelectChampSelectPlayerSelection selection : session.myTeam) {
            if (selection != null && Objects.equals(selection.summonerId, getCurrentSummoner().summonerId)) {
                //first check locked champion
                champion = Champion.getById(selection.championId);
                //if it fails check just selected champion
                if (champion == null) {
                    champion = Champion.getById(selection.championPickIntent);
                }
                //if all fails check list of actions
                if (champion == null && action != null) {
                    champion = Champion.getById(((Double) action.get("championId")).intValue());
                }
                break;
            }
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public GameMap getMap() {
        return map;
    }

    public boolean isChampionLocked() {
        if (action != null && action.containsKey("completed")) {
            return ((boolean) action.get("completed"));
        }
        return false;
    }

    private void updateGameMode() {
        if (!DebugConsts.MOCK_SESSION) {
            try {
                LolLobbyLobbyDto lolLobbyLobbyDto = getApi().executeGet("/lol-lobby/v2/lobby", LolLobbyLobbyDto.class);
                map = GameMap.getById(lolLobbyLobbyDto.gameConfig.mapId);
                positionSelector = lolLobbyLobbyDto.gameConfig.showPositionSelector;
                gameMode = GameMode.valueOf(lolLobbyLobbyDto.gameConfig.gameMode);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Exception thrown when updating the gamemode! GameMode.java might not be updated. " +
                        e.getMessage());
                if (Countly.isInitialized()) {
                    AnalyticsUtil.addCrashReport(e, "Exception thrown when updating the gamemode! GameMode.java might not be updated.", false);
                }
                positionSelector = false;
                gameMode = GameMode.CLASSIC;
                map = GameMap.MAP_11;
            }
        }
        else {
            //mock classic game without positions selector
            gameMode = GameMode.CLASSIC;
            map = GameMap.MAP_11;
            positionSelector = false;
        }
    }

    @Subscribe(ClientEventListener.ChampionSelectionEvent.NAME)
    public void onSession(ClientEventListener.ChampionSelectionEvent event) {
        if (event.getEventType() == ClientEventListener.WebSocketEventType.DELETE) {
            clearSession();
            EventBus.publish(ChampionSelectionEndEvent.NAME, new ChampionSelectionEndEvent());
        }
        else {
            Champion oldChampion = champion;
            updateGameMode();
            if (gameMode.isDisabled()) {
                return;
            }
            findCurrentAction(event.getData());
            findSelectedChampion(event.getData());
            LolChampSelectChampSelectTimer timer = event.getData().timer;
            if (timer != null) {
                currentPhase = timer.phase;
                phaseEnd = timer.internalNowInEpochMs + timer.adjustedTimeLeftInPhase;
            }
            if (event.getEventType() == ClientEventListener.WebSocketEventType.CREATE || oldChampion != champion) {
                EventBus.publish(ChampionChangedEvent.NAME, new ChampionChangedEvent(champion));
            }
        }
    }

    @Subscribe(ClientEventListener.CurrentSummonerEvent.NAME)
    public void onCurrentSummoner(ClientEventListener.CurrentSummonerEvent event) {
        resetSummoner();
    }

    public void selectChampion(Champion champion) {
        if (action == null) {
            return;
        }
        try {
            if (getGameMode() == GameMode.ONEFORALL) {
                getApi().executePost("/lol-champ-select/v1/current-champion", champion.getId());
            }
            else {
                action.put("championId", champion.getId());
                action.put("completed", false);
                getApi().executePatch("/lol-champ-select/v1/session/actions/" +
                        ((Double) action.get("id")).intValue(), action);
            }
        } catch (IOException e) {
            log.error("Exception occurred while soft picking champion", e);
        }
    }

    public void sendMessageToChampSelect(String msg) {
        try {
            LolChampSelectChampSelectSession session = getApi()
                    .executeGet("/lol-champ-select/v1/session", LolChampSelectChampSelectSession.class);
            if (session == null) {
                return;
            }
            String name = session.chatDetails.chatRoomName;
            if (name == null) {
                return;
            }
            name = name.substring(0, name.indexOf('@'));
            LolChatConversationMessageResource message = new LolChatConversationMessageResource();
            message.body = msg;
            message.type = "chat";
            try {
                getApi().executePost("/lol-chat/v1/conversations/" + name + "/messages", message);
            } catch (IOException e) {
                log.error("Exception occurred while sending a message", e);
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting a champion selection session", e);
        }
    }

    public boolean isPositionSelector() {
        return positionSelector;
    }

    public void clearSession() {
        champion = null;
        action = null;
        positionSelector = false;
        gameMode = null;
    }

    public void reset() {
        super.reset();
        clearSession();
    }

    public String getLastGrade() {
        try {
            LolMatchHistoryMatchHistoryPlayerDelta delta =
                    getApi().executeGet("/lol-match-history/v1/delta", LolMatchHistoryMatchHistoryPlayerDelta.class);
            if (delta == null) {
                return null;
            }
            return delta.deltas.stream().map(gameDelta -> gameDelta.champMastery.grade).findFirst().orElse(null);
        } catch (IOException e) {
            log.error("Exception occurred while getting last grade", e);
            return null;
        }
    }

    public static class ChampionChangedEvent implements BusEvent {
        public static final String NAME = "ChampionChangedEvent";

        private final Champion champion;

        public ChampionChangedEvent(Champion champion) {
            this.champion = champion;
        }

        public Champion getChampion() {
            return champion;
        }
    }

    public static class ChampionSelectionEndEvent implements BusEvent {
        public static final String NAME = "ChampionSelectionEndEvent";

        public ChampionSelectionEndEvent() {
        }

    }

}
