package com.stirante.runechanger.client;

import com.stirante.eventbus.AsyncEventExecutor;
import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.util.SimplePreferences;
import generated.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Matchmaking extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Matchmaking.class);

    public Matchmaking(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(ClientEventListener.MatchmakingSearchEvent.NAME)
    public void onMatchmakingSearch(ClientEventListener.MatchmakingSearchEvent event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false)) {
            LolMatchmakingMatchmakingSearchResource data = event.getData();
            if (data != null && data.dodgeData != null &&
                    data.dodgeData.state == LolMatchmakingMatchmakingDodgeState.STRANGERDODGED &&
                    data.isCurrentlyInQueue) {
                try {
                    log.debug("restart matchmaking");
                    getApi().executeDelete("/lol-lobby/v2/lobby/matchmaking/search");
                    RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                        try {
                            Thread.sleep(1000);
                            getApi().executePost("/lol-lobby/v2/lobby/matchmaking/search");
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Subscribe(value = ClientEventListener.MatchmakingSearchStateEvent.NAME, eventExecutor = AsyncEventExecutor.class)
    public void onMatchmakingSearchState(ClientEventListener.MatchmakingSearchStateEvent event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_ACCEPT, false)) {
            if (event.getData().searchState == LolLobbyLobbyMatchmakingSearchState.FOUND) {
                log.info("Found match, waiting 3 seconds and accepting");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    LolMatchmakingMatchmakingReadyCheckResource state =
                            getApi().executeGet("/lol-matchmaking/v1/ready-check", LolMatchmakingMatchmakingReadyCheckResource.class).getResponseObject();
                    if (state.state == LolMatchmakingMatchmakingReadyCheckState.INPROGRESS
                            && state.playerResponse != LolMatchmakingMatchmakingReadyCheckResponse.DECLINED &&
                            state.playerResponse != LolMatchmakingMatchmakingReadyCheckResponse.ACCEPTED) {
                        log.info("Accepting queue");
                        getApi().executePost("/lol-matchmaking/v1/ready-check/accept");
                    }
                    else {
                        log.info("Not accepting queue, because player already " + state.playerResponse.name().toLowerCase());
                    }
                } catch (IOException e) {
                    log.error("Exception occurred while autoaccepting", e);
                }
            }
        }
    }

}
