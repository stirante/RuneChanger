package com.stirante.runechanger.sourcestore.impl;

import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.SyncingListWrapper;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class LocalSource implements RuneSource {
    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {
        pages.addAll(SimplePreferences.getRuneBookValues()
                .stream()
                .filter(runePage -> runePage.getChampion() == null || runePage.getChampion().getId() == data.getChampion().getId())
                .peek(runePage -> runePage.setSourceName(getSourceName()))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public String getSourceName() {
        return LangHelper.getLang().getString("local_source");
    }

    @Override
    public String getSourceKey() {
        return "localSource";
    }
}
