package com.stirante.runechanger.api;

import java.util.List;

public interface RuneBook {

    void save();

    List<ChampionBuild> getRuneBookValues();

    RunePage getRuneBookPage(String key);

    void addRuneBookPage(RunePage page);

    void removeRuneBookPage(String key);
}
