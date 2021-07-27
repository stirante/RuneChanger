package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.ChampionBuild;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.model.client.Modifier;
import com.stirante.runechanger.model.client.Patch;
import com.stirante.runechanger.model.client.Rune;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.model.client.SummonerSpell;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.util.SyncingListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LolalyticsSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(LolalyticsSource.class);
    private final static String CHAMPION_URL =
            "https://apix1.op.lol/mega/?ep=champion&p=d&v=9patch=%PATCH%tier=platinum_plus&queue=%QUEUE%&region=all&cid=%CHAMPIONID%&lane=%LANE%";

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
                if (jsonData == null || jsonData.summary == null) {
                    // Entering this means that API has changed, error has occurred or
                    // website doesn't have build for current combination of parameters
                    continue;
                }

                // We're adding most common and highest win rate runes
                RunePage mostCommonPage = getRunePage(champion, lane, jsonData.summary.runes.pick, RunePageType.MOST_COMMON);
                if (mostCommonPage != null) {
                    pages.add(ChampionBuild.builder(mostCommonPage).withSpells(extractSummonerSpells(jsonData.summary.sum.pick)).create());
                }

                RunePage highestWin = getRunePage(champion, lane, jsonData.summary.runes.win, RunePageType.MOST_WINS);
                if (highestWin != null) {
                    pages.add(ChampionBuild.builder(highestWin).withSpells(extractSummonerSpells(jsonData.summary.sum.win)).create());
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
                "https://lolalytics.com/lol/runes/" + champion.getName().replace("'", "").toLowerCase() +
                        "?lane=" + lane + "&patch=" + Patch.getLatest(1).get(0).toString());
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
    public String getSourceKey() {
        return "lolalytics.com";
    }

    private static class LolalyticsResult {
        public Summary summary;
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

    private List<SummonerSpell> extractSummonerSpells(SumPick pick){
        try{
            final String[] spells = pick.id.split("_");
            List<SummonerSpell> list = new ArrayList<>(2);
            list.add(SummonerSpell.getByKey(Integer.parseInt(spells[0])));
            list.add(SummonerSpell.getByKey(Integer.parseInt(spells[1])));
            return list;
        }catch (Exception e){
            log.error("Unable to parse sumoner spells for {}", pick);
            return null;
        }

    }

    public static void main(String[] args) throws IOException {
        SourceStore.testSource(new LolalyticsSource(), GameMode.CLASSIC);
    }

}
