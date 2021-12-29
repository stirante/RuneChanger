package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.util.SimplePreferences;
import generated.LolChatUserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Chat extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);
    private static final String CHAT_USER_EVENT = "/lol-chat/v1/me";

    public Chat(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(CHAT_USER_EVENT)
    public void onChatUser(ClientEventListener.ClientEvent<LolChatUserResource> event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.ANTI_AWAY, false) &&
                event.getData().availability.equalsIgnoreCase("away")) {
            RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                try {
                    LolChatUserResource data = new LolChatUserResource();
                    data.availability = "chat";
                    getApi().executePut(CHAT_USER_EVENT, data);
                } catch (IOException e) {
                    log.error("Exception occurred while setting availability", e);
                }
            });
        }
    }

}
