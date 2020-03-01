package com.stirante.runechanger.client;

import com.stirante.lolclient.ClientApi;
import generated.LolSummonerSummoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class ClientModule {

    private static final Logger log = LoggerFactory.getLogger(ClientModule.class);
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
            log.error("Exception occurred while getting current summoner", e);
        }
        return currentSummoner;
    }

    public void reset() {
        resetSummoner();
    }

    public void resetSummoner() {
        currentSummoner = null;
    }

}
