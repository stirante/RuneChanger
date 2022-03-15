package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.utils.AsyncTask;
import com.stirante.runechanger.utils.SimplePreferences;
import generated.LolChatUserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Chat extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);
    private static final String CHAT_USER_EVENT = "/lol-chat/v1/me";

    public Chat(RuneChangerApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(CHAT_USER_EVENT)
    public void onChatUser(ClientEventListener.ClientEvent<LolChatUserResource> event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.ANTI_AWAY, false) &&
                event.getData().availability.equalsIgnoreCase("away")) {
            AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                try {
                    LolChatUserResource data = new LolChatUserResource();
                    data.availability = "chat";
                    getClientApi().executePut(CHAT_USER_EVENT, data);
                } catch (IOException e) {
                    log.error("Exception occurred while setting availability", e);
                }
            });
        }
    }

}
