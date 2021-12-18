package com.stirante.runechanger.client;

import com.google.gson.Gson;
import com.stirante.eventbus.BusEvent;
import com.stirante.eventbus.EventBus;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.runechanger.DebugConsts;
import generated.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventListener implements ClientWebSocket.SocketListener {
    private static final Logger log = LoggerFactory.getLogger(ClientEventListener.class);

    @Override
    public void onEvent(ClientWebSocket.Event event) {
        //printing every event except voice for experimenting
        if (DebugConsts.PRINT_EVENTS && !event.getUri().toLowerCase().contains("voice")) {
            log.info("Event: " + event);
            if (DebugConsts.PRINT_EVENTS_DATA) {
                log.debug(new Gson().toJson(event.getData()));
            }
        }
        if (event.getUri().equalsIgnoreCase("/lol-gameflow/v1/gameflow-phase")) {
            EventBus.publish(GamePhaseEvent.NAME, new GamePhaseEvent(event));
        }
        if (event.getUri().equalsIgnoreCase("/lol-chat/v1/me")) {
            EventBus.publish(ChatUserEvent.NAME, new ChatUserEvent(event));
        }
        else if (event.getUri().equalsIgnoreCase("/lol-champ-select/v1/session")) {
            EventBus.publish(ChampionSelectionEvent.NAME, new ChampionSelectionEvent(event));
        }
        else if (event.getUri().equalsIgnoreCase("/lol-matchmaking/v1/search")) {
            EventBus.publish(MatchmakingSearchEvent.NAME, new MatchmakingSearchEvent(event));
        }
        else if (event.getUri().equalsIgnoreCase("/riotclient/zoom-scale")) {
            EventBus.publish(ClientZoomScaleEvent.NAME, new ClientZoomScaleEvent(event));
        }
        else if (event.getUri().equalsIgnoreCase("/lol-summoner/v1/current-summoner")) {
            EventBus.publish(CurrentSummonerEvent.NAME, new CurrentSummonerEvent(event));
        }
        else if (event.getUri().equalsIgnoreCase("/lol-perks/v1/pages")) {
            EventBus.publish(RunePagesEvent.NAME, new RunePagesEvent(event));
        }
    }

    @Override
    public void onClose(int i, String s) {
        EventBus.publish(SocketCloseEvent.NAME, new SocketCloseEvent());
    }

    public enum WebSocketEventType {
        CREATE,
        UPDATE,
        DELETE
    }

    public static class WebSocketEvent<T> implements BusEvent {

        protected final WebSocketEventType eventType;
        protected final T data;

        public WebSocketEvent(ClientWebSocket.Event event) {
            eventType = WebSocketEventType.valueOf(event.getEventType().toUpperCase());
            //noinspection unchecked
            data = (T) event.getData();
        }

        public WebSocketEvent(WebSocketEventType eventType, T data) {
            this.eventType = eventType;
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public WebSocketEventType getEventType() {
            return eventType;
        }
    }

    public static class GamePhaseEvent extends WebSocketEvent<LolGameflowGameflowPhase> {
        public static final String NAME = "GamePhaseEvent";

        public GamePhaseEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public GamePhaseEvent(WebSocketEventType eventType, LolGameflowGameflowPhase data) {
            super(eventType, data);
        }

    }

    public static class ChatUserEvent extends WebSocketEvent<LolChatUserResource> {
        public static final String NAME = "ChatUserEvent";

        public ChatUserEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public ChatUserEvent(WebSocketEventType eventType, LolChatUserResource data) {
            super(eventType, data);
        }

    }

    public static class ChampionSelectionEvent extends WebSocketEvent<LolChampSelectChampSelectSession> {
        public static final String NAME = "ChampionSelectionEvent";

        public ChampionSelectionEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public ChampionSelectionEvent(WebSocketEventType eventType, LolChampSelectChampSelectSession data) {
            super(eventType, data);
        }

    }

    public static class MatchmakingSearchEvent extends WebSocketEvent<LolMatchmakingMatchmakingSearchResource> {
        public static final String NAME = "MatchmakingSearchEvent";

        public MatchmakingSearchEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public MatchmakingSearchEvent(WebSocketEventType eventType, LolMatchmakingMatchmakingSearchResource data) {
            super(eventType, data);
        }

    }

    public static class ClientZoomScaleEvent extends WebSocketEvent<Object> {
        public static final String NAME = "ClientZoomScaleEvent";

        public ClientZoomScaleEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public ClientZoomScaleEvent(WebSocketEventType eventType, Object data) {
            super(eventType, data);
        }

    }

    public static class CurrentSummonerEvent extends WebSocketEvent<LolSummonerSummoner> {
        public static final String NAME = "CurrentSummonerEvent";

        public CurrentSummonerEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public CurrentSummonerEvent(WebSocketEventType eventType, LolSummonerSummoner data) {
            super(eventType, data);
        }

    }

    public static class RunePagesEvent extends WebSocketEvent<LolPerksPerkPageResource[]> {
        public static final String NAME = "RunePagesEvent";

        public RunePagesEvent(ClientWebSocket.Event event) {
            super(event);
        }

        public RunePagesEvent(WebSocketEventType eventType, LolPerksPerkPageResource[] data) {
            super(eventType, data);
        }

    }

    public static class SocketCloseEvent implements BusEvent {
        public static final String NAME = "SocketCloseEvent";

        public SocketCloseEvent() {
        }

    }

}
