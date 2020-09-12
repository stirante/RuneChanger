package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.SyncingListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class LolalyticsSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(LolalyticsSource.class);
    private final static String CHAMPION_URL =
            "https://api.op.lol/champion/3/?patch=%PATCH%tier=platinum_plus&queue=420&region=all&cid=%CHAMPIONID%&lane=%LANE%";

    private void downloadRunes(Champion champion, SyncingListWrapper<RunePage> pages) {
        final String[] lanes = {"Top", "Jungle", "Middle", "Bottom", "Support"};
        try {
            for (String lane : lanes) {
                final URL url = new URL(CHAMPION_URL
                        .replace("%PATCH", Patch.getLatest(1).get(0).toString())
                        .replace("%CHAMPIONID%", Integer.toString(champion.getId()))
                        .replace("%LANE%", lane.toLowerCase()));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                LolalyticsResult jsonData =
                        new Gson().fromJson(new InputStreamReader(conn.getInputStream()), LolalyticsResult.class);
                conn.getInputStream().close();
                ConvertedDataPair convertedDataPair = new ConvertedDataPair(Map.ofEntries(
                        Map.entry("rune1", jsonData.display.rune1),
                        Map.entry("rune2", jsonData.display.rune2),
                        Map.entry("rune3", jsonData.display.rune3),
                        Map.entry("rune4", jsonData.display.rune4),
                        Map.entry("rune5", jsonData.display.rune5)
                ));
                RunePage runePage = calculateRunes(convertedDataPair, RunePageType.MOST_COMMON);
                if (!runePage.verify()) {
                    log.error("Runepage has failed verification. Mode: " + RunePageType.MOST_COMMON.name());
                    return;
                }
                runePage.setChampion(champion);
                runePage.setName(lane);
                runePage.setSourceName(this.getSourceName());
                runePage.setSource(
                        "https://lolalytics.com/lol/runes/" + champion.getName().replace("'", "").toLowerCase() +
                                "?lane=" + lane + "&patch=" + Patch.getLatest(1).get(0).toString());
                pages.add(runePage);
            }

        } catch (Exception e) {
            log.error("Exception occurred while getting Lolalytics/op.lol source data", e);
        }
    }


    private RunePage calculateRunes(ConvertedDataPair convertedDataPair, RunePageType mode) {
        RunePage r = new RunePage();

        // PRIMARY RUNES
        //Checking which keystone has the biggest number of plays/wins (depending on mode)
        Map.Entry<Rune, int[]> biggestValKeystone = convertedDataPair.getRuneDataConverted().get("rune1")
                .entrySet()
                .stream()
                .filter(map -> map.getKey().getSlot() == 0)
                .max(Comparator.comparingInt(runeEntry -> runeEntry.getValue()[mode.getIndex()]))
                .orElseThrow();

        final Style primaryStyle = biggestValKeystone.getKey().getStyle();
        r.setMainStyle(primaryStyle);
        r.getRunes().add(biggestValKeystone.getKey());

        //Checking which runes are we still able to use
        List<Map.Entry<Rune, int[]>> availablePrimaryRunes =
                convertedDataPair.getRuneDataConverted().get("rune1").entrySet().stream()
                        .filter(map -> map.getKey().getStyle() == primaryStyle)
                        .collect(Collectors.toList());
        //Checking for a highest scoring rune for each slot
        for (int i = 1; i < 4; i++) {
            final int slot = i;
            Map.Entry<Rune, int[]> biggestValRune = availablePrimaryRunes.stream()
                    .filter(map -> map.getKey().getSlot() == slot)
                    .max(Comparator.comparingInt(runeEntry -> runeEntry.getValue()[mode.getIndex()]))
                    .orElseThrow();
            r.getRunes().add(biggestValRune.getKey());
        }
        // SECONDARY RUNES

        Map.Entry<Rune, int[]> biggestValSecondaryRune = convertedDataPair.getRuneDataConverted().get("rune2")
                .entrySet()
                .stream()
                .max(Comparator.comparingInt(runeEntry -> runeEntry.getValue()[mode.getIndex()]))
                .orElseThrow();
        final Style secondaryStyle = biggestValSecondaryRune.getKey().getStyle();
        final int secondaryUsedSlot = biggestValSecondaryRune.getKey().getSlot();
        r.setSubStyle(secondaryStyle);
        r.getRunes().add(biggestValSecondaryRune.getKey());

        Map.Entry<Rune, int[]> biggestRemainingSecondaryRune = convertedDataPair.getRuneDataConverted().get("rune2").entrySet().stream()
                .filter(map -> map.getKey().getStyle() == secondaryStyle && map.getKey().getSlot() != secondaryUsedSlot)
                .max(Comparator.comparingInt(runeEntry -> runeEntry.getValue()[mode.getIndex()]))
                .orElseThrow();
        r.getRunes().add(biggestRemainingSecondaryRune.getKey());

        // MODIFIERS

        for (int i = 3; i < 6; i++) {
            Map<Modifier, int[]> modifierList = convertedDataPair.getModifierDataConverted().get("rune" + i);
            Map.Entry<Modifier, int[]> biggestValModifier = modifierList.entrySet().stream()
                    .max(Comparator.comparingInt(runeEntry -> runeEntry.getValue()[mode.getIndex()]))
                    .orElseThrow();
            r.getModifiers().add(biggestValModifier.getKey());
        }
        return r;
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {
        downloadRunes(data.getChampion(), pages);
    }

    @Override
    public String getSourceName() {
        return "Lolalytics";
    }

    @Override
    public String getSourceKey() {
        return "lolalytics.com";
    }

    private static class LolalyticsResult {
        public DisplayRuneRawData display;
    }

    private static class DisplayRuneRawData {
        public Map<String, int[]> rune1;
        public Map<String, int[]> rune2;
        public Map<String, int[]> rune3;
        public Map<String, int[]> rune4;
        public Map<String, int[]> rune5;
    }

    private static class ConvertedDataPair {
        public Map<String, Map<Rune, int[]>> runeDataConverted;
        public Map<String, Map<Modifier, int[]>> modifierDataConverted;

        private ConvertedDataPair(Map<String, Map<String, int[]>> rawData) {
            Map<String, Map<Rune, int[]>> runeDataConverted = new HashMap<>();
            Map<String, Map<Modifier, int[]>> modifierDataConverted = new HashMap<>();

            //converting runes
            for (int i = 0; i < 2; i++) {
                int n = i + 1;
                runeDataConverted.put("rune" + n, rawData.get("rune" + n)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> Rune.getById(Integer.parseInt(e.getKey())), Map.Entry::getValue)));
            }

            //converting modifiers
            for (int i = 2; i < 5; i++) {
                int n = i + 1;
                modifierDataConverted.put("rune" + n, rawData.get("rune" + n)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())), Map.Entry::getValue)));
            }

            this.runeDataConverted = runeDataConverted;
            this.modifierDataConverted = modifierDataConverted;
        }

        public Map<String, Map<Rune, int[]>> getRuneDataConverted() {
            return this.runeDataConverted;
        }

        public Map<String, Map<Modifier, int[]>> getModifierDataConverted() {
            return this.modifierDataConverted;
        }
    }

    public enum RunePageType {
        MOST_COMMON(0),
        MOST_WINS(1);
        private final int index;

        RunePageType(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

}
