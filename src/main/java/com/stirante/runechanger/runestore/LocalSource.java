package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.collections.ObservableList;

public class LocalSource implements RuneSource {
    @Override
    public void getForChampion(Champion champion, ObservableList<RunePage> pages) {
        FxUtils.doOnFxThread(() -> pages.addAll(SimplePreferences.getRuneBookValues()));
    }

    @Override
    public String getSourceName() {
        return "Local";
    }
}
