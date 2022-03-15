package com.stirante.runechanger.client;

import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.api.Champions;
import com.stirante.runechanger.api.RuneChangerApi;
import generated.LolSummonerSummoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class ClientModule {

    private static final Logger log = LoggerFactory.getLogger(ClientModule.class);
    private final RuneChangerApi api;
    private LolSummonerSummoner currentSummoner;

    public static final String CURRENT_SUMMONER_EVENT = "/lol-summoner/v1/current-summoner";

    public ClientModule(RuneChangerApi api) {
        this.api = api;
    }

    public ClientApi getClientApi() {
        return api.getClientApi();
    }

    public RuneChangerApi getApi() {
        return api;
    }

    public Champions getChampions() {
        return api.getChampions();
    }

    public LolSummonerSummoner getCurrentSummoner() {
        try {
            if (currentSummoner == null) {
                currentSummoner = getClientApi().executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class).getResponseObject();
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
