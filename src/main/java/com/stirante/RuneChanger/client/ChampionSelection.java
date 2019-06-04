package com.stirante.RuneChanger.client;

import com.stirante.RuneChanger.DebugConsts;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.GameMode;
import com.stirante.lolclient.ClientApi;
import generated.*;

import java.io.IOException;
import java.util.*;

public class ChampionSelection extends ClientModule {

    private Map<String, Object> action;
    private Champion champion;
    private GameMode gameMode;
    private boolean positionSelector;

    public ChampionSelection(ClientApi api) {
        super(api);
    }

    public ArrayList<Champion> getLastChampions() {
        try {
            HashSet<Champion> lastChampions = new HashSet<>();
            LolMatchHistoryMatchHistoryList historyList =
                    getApi().executeGet("/lol-match-history/v2/matchlist?begIndex=0&endIndex=20",
                            LolMatchHistoryMatchHistoryList.class);
            for (LolMatchHistoryMatchHistoryGame game : historyList.games.games) {
                LolMatchHistoryMatchHistoryParticipant p = game.participants.stream()
                        .findFirst()
                        .orElse(null);
                if (p == null) {
                    continue;
                }
                lastChampions.add(Champion.getById(p.championId));
            }
            return new ArrayList<>(lastChampions);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Champion getSelectedChampion() {
        return champion;
    }

    @SuppressWarnings("unchecked")
    private void findCurrentAction(LolChampSelectChampSelectSession session) {
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
                }
            }
        }
    }

    private void findSelectedChampion(LolChampSelectChampSelectSession session) {
        //find selected champion
        for (LolChampSelectChampSelectPlayerSelection selection : session.myTeam) {
            if (Objects.equals(selection.summonerId, getCurrentSummoner().summonerId)) {
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
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public boolean isChampionLocked() {
        if (action != null && action.containsKey("completed")) {
            return ((boolean) action.get("completed"));
        }
        return false;
    }

    private void updateGameMode() {
        try {
            LolLobbyLobbyDto lolLobbyLobbyDto = getApi().executeGet("/lol-lobby/v2/lobby", LolLobbyLobbyDto.class);
            gameMode = GameMode.valueOf(lolLobbyLobbyDto.gameConfig.gameMode);
            positionSelector = lolLobbyLobbyDto.gameConfig.showPositionSelector;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSession(LolChampSelectChampSelectSession session) {
        findCurrentAction(session);
        findSelectedChampion(session);
        updateGameMode();
    }


    public void selectChampion(Champion champion) {
        if (action == null) {
            return;
        }
        try {
            action.put("championId", champion.getId());
            action.put("completed", false);
            getApi().executePatch("/lol-champ-select/v1/session/actions/" +
                    ((Double) action.get("id")).intValue(), action);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToChampSelect(String msg) {
        try {
            LolChampSelectChampSelectSession session = getApi()
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
                getApi().executePost("/lol-chat/v1/conversations/" + name + "/messages", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
