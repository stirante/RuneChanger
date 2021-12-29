package com.stirante.runechanger.client;

import com.google.gson.Gson;
import com.stirante.eventbus.EventBus;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.runechanger.DebugConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventListener implements ClientWebSocket.SocketListener {
    private static final Logger log = LoggerFactory.getLogger(ClientEventListener.class);

    public static final String SOCKET_CLOSE_EVENT = "SocketCloseEvent";

    public static final String EVENT_TYPE_CREATE = "create";
    public static final String EVENT_TYPE_UPDATE = "update";
    public static final String EVENT_TYPE_DELETE = "delete";

    @Override
    public void onEvent(ClientWebSocket.Event event) {
        //printing every event except voice for experimenting
        if (DebugConsts.PRINT_EVENTS && !event.getUri().toLowerCase().contains("voice")) {
            log.info("Event: " + event);
            if (DebugConsts.PRINT_EVENTS_DATA) {
                log.debug(new Gson().toJson(event.getData()));
            }
        }
        EventBus.publish(event.getUri(), new ClientEvent<>(event));
    }

    @Override
    public void onClose(int i, String s) {
        EventBus.publish(SOCKET_CLOSE_EVENT);
    }

    public static class ClientEvent<T> {
        private final WebSocketEventType type;
        private final ClientWebSocket.Event event;

        public ClientEvent(ClientWebSocket.Event event) {
            this.type = WebSocketEventType.valueOf(event.getEventType().toUpperCase());
            this.event = event;
        }

        protected ClientEvent(WebSocketEventType type) {
            this.type = type;
            this.event = null;
        }

        @SuppressWarnings("unchecked")
        public T getData() {
            return (T) event.getData();
        }

        public WebSocketEventType getEventType() {
            return type;
        }

    }

    public static class DummyClientEvent<T> extends ClientEvent<T> {
        private final T data;

        public DummyClientEvent(WebSocketEventType type, T data) {
            super(type);
            this.data = data;
        }

        @Override
        public T getData() {
            return data;
        }
    }

    public enum WebSocketEventType {
        CREATE, UPDATE, DELETE
    }

}
