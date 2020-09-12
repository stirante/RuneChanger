package com.stirante.runechanger.sourcestore.impl;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.sourcestore.RuneSource;
import javafx.collections.ObservableList;

public class MetasrcSource implements RuneSource {
    @Override
    public void getRunesForChampion(Champion champion, GameMode mode, ObservableList<RunePage> pages) {

    }

    @Override
    public String getSourceName() {
        return "Metasrc";
    }

    @Override
    public String getSourceKey() {
        return "metasrc.com";
    }
}
