package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.api.*;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.Patch;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.utils.SyncingListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LolalyticsSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(LolalyticsSource.class);
    private final static String CHAMPION_URL =
            "https://axe.lolalytics.com/mega/?ep=champion&p=d&v=1&patch=%PATCH%tier=platinum_plus&queue=%QUEUE%&region=all&cid=%CHAMPIONID%&lane=%LANE%&region=all";
    private int minThreshold = 0;
    private boolean mostCommon = false;

    private void downloadRunes(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        Champion champion = data.getChampion();
        final String[] lanes = {"Top", "Jungle", "Middle", "Bottom", "Support"};

        try {
            for (String lane : lanes) {
                final URL url = new URL(CHAMPION_URL
                        .replace("%PATCH", Patch.getLatest(1).get(0).toString())
                        .replace("%CHAMPIONID%", Integer.toString(champion.getId()))
                        .replace("%LANE%", lane.toLowerCase())
                        .replace("%QUEUE%", getQueueId(data.getGameMode())));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                StringWriter json = new StringWriter();
                Pipe.from(conn.getInputStream()).to(json);
                LolalyticsResult jsonData =
                        new Gson().fromJson(json.toString(), LolalyticsResult.class);
                if (jsonData == null || jsonData.summary == null || jsonData.header == null ||
                        jsonData.header.n < minThreshold) {
                    // Entering this means that API has changed, error has occurred or
                    // website doesn't have build for current combination of parameters
                    // or current combination of parameters doesn't satisfy requirements
                    continue;
                }

                // We're adding most common or highest win rate runes depending on the settings
                RunePage runePage =
                        getRunePage(champion, lane, jsonData.summary.runes.pick, mostCommon ? RunePageType.MOST_COMMON : RunePageType.MOST_WINS);
                if (runePage != null) {
                    pages.add(ChampionBuild.builder(runePage)
                            .withSpells(extractSummonerSpells(jsonData.summary.sum.pick))
                            .create());
                }
            }

        } catch (Exception e) {
            log.error("Exception occurred while getting Lolalytics/op.lol source data", e);
        }
    }

    private RunePage getRunePage(Champion champion, String lane, Pick runesRaw, RunePageType runePageType) {
        RunePage runePage = calculateRunes(runesRaw);
        if (!runePage.verify()) {
            log.error("Runepage has failed verification.");
            return null;
        }
        runePage.setChampion(champion);
        runePage.setName(lane);
        runePage.setSourceName(this.getSourceName(runePageType));
        runePage.setSource(
                "https://lolalytics.com/lol/" + champion.getName().replace("'", "").toLowerCase() +
                        "/build/?lane=" + lane);
        return runePage;
    }


    private RunePage calculateRunes(Pick pick) {
        RunePage r = new RunePage();

        //Adding primary
        for (int i : pick.set.pri) {
            r.getRunes().add(Rune.getById(i));
        }

        //Adding secondary
        for (int i : pick.set.sec) {
            r.getRunes().add(Rune.getById(i));
        }

        //Adding modifiers
        for (int i : pick.set.mod) {
            r.getModifiers().add(Modifier.getById(i));
        }

        r.setMainStyle(r.getRunes().get(0).getStyle());
        r.setSubStyle(r.getRunes().get(4).getStyle());

        return r;
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        downloadRunes(data, pages);
    }

    @Override
    public String getSourceName() {
        return "Lolalytics";
    }

    // Imports both highest win rate and most common build until checkbox in settings is implemented
    public String getSourceName(RunePageType runePageType) {
        return "Lolalytics - " + runePageType.name;
    }

    @Override
    public GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.ARAM, GameMode.URF, GameMode.ONEFORALL, GameMode.ULTBOOK, GameMode.CLASSIC};
    }

    @Override
    public void onSettingsUpdate(Map<String, Object> settings) {
        if (settings.containsKey("min_threshold")) {
            minThreshold = Integer.parseInt((String) settings.get("min_threshold"));
        }
        if (settings.containsKey("most_common")) {
            mostCommon = (Boolean) settings.get("most_common");
        }
    }

    @Override
    public void setupSettings(SettingsConfiguration config) {
        config
                .textField("min_threshold")
                .defaultValue("50")
                .validation(s -> {
                    try {
                        return Integer.parseInt(s) >= 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .add()
                .checkbox("most_common")
                .defaultValue(false)
                .add();
    }

    @Override
    public String getSourceKey() {
        return "lolalytics.com";
    }

    private static class LolalyticsResult {
        public Summary summary;
        public Header header;
    }

    private static class Header {
        public int n;
        public String tier;
    }

    private static class Summary {
        public Runes runes;
        public Sum sum;
    }

    private static class Runes {
        public Pick pick;
        public Pick win;
    }

    private static class Pick {
        public SetLol set;
    }

    private static class SetLol {
        public int[] pri;
        public int[] sec;
        public int[] mod;
    }

    private static class Sum {
        public SumPick pick;
        public SumPick win;
    }

    private static class SumPick {
        public String id;
    }

    public enum RunePageType {
        MOST_COMMON("Most common"),
        MOST_WINS("Highest win");
        private final String name;

        RunePageType(String name) {
            this.name = name;
        }

        public String getIndex() {
            return name;
        }
    }

    private String getQueueId(GameMode mode) {
        switch (mode) {
            case ARAM:
                return "450";
            case URF:
                return "900";
            case ONEFORALL:
                return "1020";
            case ULTBOOK:
                return "1400";
            default:
                log.info("Game mode {} not available for this source. Returning runes for solo/duo.", mode.getName());
            case CLASSIC:
                return "420";
        }
    }

    private List<SummonerSpell> extractSummonerSpells(SumPick pick) {
        try {
            final String[] spells = pick.id.split("_");
            List<SummonerSpell> list = new ArrayList<>(2);
            list.add(SummonerSpell.getByKey(Integer.parseInt(spells[0])));
            list.add(SummonerSpell.getByKey(Integer.parseInt(spells[1])));
            return list;
        } catch (Exception e) {
            log.error("Unable to parse sumoner spells for {}", pick);
            return null;
        }

    }

    public static void main(String[] args) throws IOException {
        SourceStore.testSource(new LolalyticsSource());
    }

}
