package com.stirante.runechanger.api;

import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.api.overlay.ClientOverlay;

public interface RuneChangerApi {

    RuneBook getRuneBook();

    String getVersion();

    ClientOverlay getClientOverlay();

    ClientApi getClientApi();

    Champions getChampions();

//    SourceStore getSourceStore();

}
