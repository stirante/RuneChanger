package com.stirante.RuneChanger.client;

import com.stirante.lolclient.ClientApi;
import generated.LolLootPlayerLoot;

import java.io.IOException;

public class Login extends ClientModule {
    public Login(ClientApi api) {
        super(api);
    }

    /**
     * Kills login session and restarts UI of the client
     * @throws IOException
     */
    public void relogin() throws IOException {
        getApi().executeDelete("/lol-login/v1/session");
        getApi().executePost("/riotclient/kill-and-restart-ux");
    }

}
