package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class LocalSource implements RuneSource {
    @Override
    public void getForChampion(Champion champion, ObservableList<RunePage> pages) {
        FxUtils.doOnFxThread(() -> pages.addAll(SimplePreferences.getRuneBookValues()
                .stream()
                .filter(runePage -> runePage.getChampion() == null || runePage.getChampion() == champion)
                .collect(Collectors.toCollection(ArrayList::new))));
    }

    @Override
    public String getSourceName() {
        return "Local";
    }
}
