package com.stirante.runechanger.sourcestore.impl;

import com.stirante.runechanger.model.client.ChampionBuild;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.RuneBook;
import com.stirante.runechanger.utils.SyncingListWrapper;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class LocalSource implements RuneSource {
    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        pages.addAll(RuneBook.getRuneBookValues()
                .stream()
                .filter(runePage -> runePage.getChampion() == null ||
                        runePage.getChampion().getId() == data.getChampion().getId())
                .peek(runePage -> runePage.setSourceName(getSourceName()))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public String getSourceName() {
        return com.stirante.runechanger.RuneChanger.getInstance().getLang().getString("local_source");
    }

    @Override
    public String getSourceKey() {
        return "localSource";
    }
}
