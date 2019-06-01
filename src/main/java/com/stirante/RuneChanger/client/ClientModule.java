package com.stirante.RuneChanger.client;

import com.stirante.lolclient.ClientApi;
import generated.LolSummonerSummoner;

import java.io.IOException;

public abstract class ClientModule {

    private final ClientApi api;
    private LolSummonerSummoner currentSummoner;

    public ClientModule(ClientApi api) {
        this.api = api;
    }

    public ClientApi getApi() {
        return api;
    }

    public LolSummonerSummoner getCurrentSummoner() {
        try {
            if (currentSummoner == null) {
                currentSummoner = api.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentSummoner;
    }

}
