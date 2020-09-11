package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.FxUtils;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LolalyticsSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);
    private final static String CHAMPION_URL = "https://api.op.lol/champion/3/?patch=%PATCH%tier=platinum_plus&queue=420&region=all&cid=%CHAMPIONID%&lane=%LANE%";
    private static final int TIMEOUT = 10000;
    private Map<Rune, int[]> runeMap;

    private void downloadRunes(Champion champion, ObservableList<RunePage> pages) {
        final String[] lanes = {"Top", "Jungle", "Middle", "Bottom", "Support"};
        try {
            for (String lane : lanes) {
                final URL url = new URL(CHAMPION_URL
                        .replace("%PATCH", Patch.getLatest(1).get(0).toString())
                        .replace("%CHAMPIONID%", Integer.toString(champion.getId()))
                        .replace("%LANE%", lane.toLowerCase()));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                LolalyticsResult jsonData = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), LolalyticsResult.class);
                conn.getInputStream().close();
                ConvertedDataPair convertedDataPair = convertRunes(jsonData.display);
                RunePage runePage = calculateRunes(convertedDataPair, RunePageType.MOST_COMMON);
                if(runePage != null) {
                    runePage.setChampion(champion);
                    runePage.setName(lane);
                    runePage.setSourceName(this.getSourceName());
                    runePage.setSource("https://lolalytics.com/");
                    FxUtils.doOnFxThread(() -> pages.add(runePage));
                }
            }

        } catch (Exception e) {
            log.error("Exception occurred while getting Lolalytics/op.lol source data", e);
        }
    }

    private ConvertedDataPair convertRunes(Display display) {
        Map<String, Map<Rune, int[]>> runeDataConverted = new HashMap<>();
        Map<String, Map<Modifier, int[]>> modifierDataConverted = new HashMap<>();
        runeDataConverted.put("rune1", display.rune1.entrySet().stream().collect(Collectors.toMap(e -> Rune.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue)));
        runeDataConverted.put("rune2", display.rune2.entrySet().stream().collect(Collectors.toMap(e -> Rune.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue)));
        modifierDataConverted.put("rune3", display.rune3.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue)));
        modifierDataConverted.put("rune4", display.rune4.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue)));
        modifierDataConverted.put("rune5", display.rune5.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue)));
        return new ConvertedDataPair(runeDataConverted, modifierDataConverted);
    }

    private RunePage calculateRunes(ConvertedDataPair convertedDataPair, RunePageType mode) {
        //Modes: 0 - most common, 1 - most wins
        RunePage r = new RunePage();
        // PRIMARY RUNES
        Map<Rune, int[]> keystones = convertedDataPair.getRuneDataConverted().get("rune1").entrySet().stream()
                .filter(map -> map.getKey().getSlot() == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map.Entry<Rune, int[]> biggestValKeystone = null;
        //Checking which keystone has the biggest number of plays/wins (depending on mode)
        for (Map.Entry<Rune, int[]> entry : keystones.entrySet()) {
            if (biggestValKeystone == null || entry.getValue()[mode.getIndex()] > biggestValKeystone.getValue()[mode.getIndex()]) {
                biggestValKeystone = entry;
            }
        }
        final Style primaryStyle = Objects.requireNonNull(biggestValKeystone).getKey().getStyle();
        r.setMainStyle(primaryStyle);
        r.getRunes().add(biggestValKeystone.getKey());

        //Checking which runes are we still able to use
        Map<Rune, int[]> availablePrimaryRunes = convertedDataPair.getRuneDataConverted().get("rune1").entrySet().stream()
                .filter(map -> map.getKey().getStyle() == primaryStyle)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        //Checking for a highest scoring rune for each slot
        for (int i = 1; i < 4; i++) {
            final int slot = i;
            Map<Rune, int[]> slotRunes = availablePrimaryRunes.entrySet().stream()
                    .filter(map -> map.getKey().getSlot() == slot)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map.Entry<Rune, int[]> biggestValRune = null;
            for (Map.Entry<Rune, int[]> entry : slotRunes.entrySet()) {
                if (biggestValRune == null || entry.getValue()[mode.getIndex()] > biggestValRune.getValue()[mode.getIndex()]) {
                    biggestValRune = entry;
                }
            }
            r.getRunes().add(Objects.requireNonNull(biggestValRune).getKey());
        }
        // SECONDARY RUNES

        Map.Entry<Rune, int[]> biggestValSecondaryRune = null;
        for (Map.Entry<Rune, int[]> entry : convertedDataPair.getRuneDataConverted().get("rune2").entrySet()) {
            if (biggestValSecondaryRune == null || entry.getValue()[mode.getIndex()] > biggestValSecondaryRune.getValue()[mode.getIndex()]) {
                biggestValSecondaryRune = entry;
            }
        }
        final Style secondaryStyle = Objects.requireNonNull(biggestValSecondaryRune).getKey().getStyle();
        final int secondaryUsedSlot = biggestValSecondaryRune.getKey().getSlot();
        r.setSubStyle(secondaryStyle);
        r.getRunes().add(biggestValSecondaryRune.getKey());

        Map<Rune, int[]> remainingSecondaryRunes = convertedDataPair.getRuneDataConverted().get("rune2").entrySet().stream()
                .filter(map -> map.getKey().getStyle() == secondaryStyle && map.getKey().getSlot() != secondaryUsedSlot)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map.Entry<Rune, int[]> biggestRemainingSecondaryRune = null;
        for (Map.Entry<Rune, int[]> entry : remainingSecondaryRunes.entrySet()) {
            if (biggestRemainingSecondaryRune == null || entry.getValue()[mode.getIndex()] > biggestRemainingSecondaryRune.getValue()[mode.getIndex()]) {
                biggestRemainingSecondaryRune = entry;
            }
        }


        r.getRunes().add(Objects.requireNonNull(biggestRemainingSecondaryRune).getKey());

        //MODIFIERS


        for (int i = 3; i < 6; i++) {
            Map<Modifier, int[]> modifierList = convertedDataPair.getModifierDataConverted().get("rune" + i);
            Map.Entry<Modifier, int[]> biggestValModifier = null;
            for (Map.Entry<Modifier, int[]> entry : modifierList.entrySet()) {
                if (biggestValModifier == null || entry.getValue()[mode.getIndex()] > biggestRemainingSecondaryRune.getValue()[mode.getIndex()]) {
                    biggestValModifier = entry;
                }
            }
            r.getModifiers().add(Objects.requireNonNull(biggestValModifier).getKey());
        }

        if (!r.verify()) {
            log.error("Failed to verify the runepage.");
            return null;
        }
        return r;

    }

    @Override
    public void getRunesForChampion(Champion champion, GameMode mode, ObservableList<RunePage> pages) {
        if(champion != null) {
            downloadRunes(champion, pages);
        }
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
        private Display display;
    }

    private static class Display {
        private HashMap<String, int[]> rune1;
        private HashMap<String, int[]> rune2;
        private HashMap<String, int[]> rune3;
        private HashMap<String, int[]> rune4;
        private HashMap<String, int[]> rune5;
    }

    private class ConvertedDataPair {
        Map<String, Map<Rune, int[]>> runeDataConverted;
        Map<String, Map<Modifier, int[]>> modifierDataConverted;
        private ConvertedDataPair(Map<String, Map<Rune, int[]>> runeDataConverted, Map<String, Map<Modifier, int[]>> modifierDataConverted) {
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
