package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.FxUtils;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LolalyticsSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);
    private final static String CHAMPION_URL = "https://api.op.lol/champion/3/?patch=10.18tier=platinum_plus&queue=420&region=all";
    private static final int TIMEOUT = 10000;
    private Map<Rune, int[]> runeMap;

    public static void main(String[] args) throws IOException {

    }

    private void downloadRunes(Champion champion, ObservableList<RunePage> pages) {
        final String URL = CHAMPION_URL + "&cid=" + champion.getId() + "&lane=";
        final String[] lanes = {"Top", "Jungle", "Middle", "Bottom", "Support"};
        try {
            for (String lane : lanes) {
                HttpURLConnection conn = (HttpURLConnection) new URL(URL + lane.toLowerCase()).openConnection();
                conn.connect();
                LolalyticsResult jsonData = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), LolalyticsResult.class);
                conn.getInputStream().close();
                DisplayConverted displayConverted = convertRunes(jsonData.display);
                RunePage runePage = calculateRunes(displayConverted, 0);
                runePage.setChampion(champion);
                runePage.setName(lane);
                runePage.setSourceName(this.getSourceName());
                runePage.setSource("https://lolalytics.com/");
                FxUtils.doOnFxThread(() -> pages.add(runePage));
            }

        } catch (Exception e) {
            log.error("Exception occurred while getting Lolalytics/op.lol source data", e);
        }
    }

    private DisplayConverted convertRunes(Display display) {
        // Converting all of the results to use the Rune class
        DisplayConverted displayConverted = new DisplayConverted();
        displayConverted.rune1 = display.rune1.entrySet().stream().collect(Collectors.toMap(e -> Rune.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue));
        displayConverted.rune2 = display.rune2.entrySet().stream().collect(Collectors.toMap(e -> Rune.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue));
        displayConverted.rune3 = display.rune3.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue));
        displayConverted.rune4 = display.rune4.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue));
        displayConverted.rune5 = display.rune5.entrySet().stream().collect(Collectors.toMap(e -> Modifier.getById(Integer.parseInt(e.getKey())),
                Map.Entry::getValue));
        return displayConverted;
    }

    private RunePage calculateRunes(DisplayConverted displayConverted, int mode) {
        //Modes: 0 - most common, 1 - most wins
        RunePage r = new RunePage();
        // PRIMARY RUNES
        Map<Rune, int[]> keystones = displayConverted.rune1.entrySet().stream()
                .filter(map -> map.getKey().getSlot() == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map.Entry<Rune, int[]> biggestValKeystone = null;
        //Checking which keystone has the biggest number of plays/wins (depending on mode)
        for (Map.Entry<Rune, int[]> entry : keystones.entrySet()) {
            if (biggestValKeystone == null || entry.getValue()[mode] > biggestValKeystone.getValue()[mode]) {
                biggestValKeystone = entry;
            }
        }
        final Style primaryStyle = biggestValKeystone.getKey().getStyle();
        r.setMainStyle(primaryStyle);
        r.getRunes().add(biggestValKeystone.getKey());

        //Checking which runes are we still able to use
        Map<Rune, int[]> availablePrimaryRunes = displayConverted.rune1.entrySet().stream()
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
                if (biggestValRune == null || entry.getValue()[mode] > biggestValRune.getValue()[mode]) {
                    biggestValRune = entry;
                }
            }
            r.getRunes().add(biggestValRune.getKey());
        }
        // SECONDARY RUNES

        Map.Entry<Rune, int[]> biggestValSecondaryRune = null;
        for (Map.Entry<Rune, int[]> entry : displayConverted.rune2.entrySet()) {
            if (biggestValSecondaryRune == null || entry.getValue()[mode] > biggestValSecondaryRune.getValue()[mode]) {
                biggestValSecondaryRune = entry;
            }
        }
        final Style secondaryStyle = biggestValSecondaryRune.getKey().getStyle();
        final int secondaryUsedSlot = biggestValSecondaryRune.getKey().getSlot();
        r.setSubStyle(secondaryStyle);
        r.getRunes().add(biggestValSecondaryRune.getKey());

        Map<Rune, int[]> remainingSecondaryRunes = displayConverted.rune2.entrySet().stream()
                .filter(map -> map.getKey().getStyle() == secondaryStyle && map.getKey().getSlot() != secondaryUsedSlot)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map.Entry<Rune, int[]> biggestRemainingSecondaryRune = null;
        for (Map.Entry<Rune, int[]> entry : remainingSecondaryRunes.entrySet()) {
            if (biggestRemainingSecondaryRune == null || entry.getValue()[mode] > biggestRemainingSecondaryRune.getValue()[mode]) {
                biggestRemainingSecondaryRune = entry;
            }
        }


        r.getRunes().add(biggestRemainingSecondaryRune.getKey());

        //MODIFIERS


        for (int i = 3; i < 6; i++) {
            try {
                Field listField = DisplayConverted.class.getDeclaredField("rune" + i);
                listField.setAccessible(true);
                Map<Modifier, int[]> modifierList = (Map<Modifier, int[]>) listField.get(displayConverted);
                Map.Entry<Modifier, int[]> biggestValModifier = null;
                for (Map.Entry<Modifier, int[]> entry : modifierList.entrySet()) {
                    if (biggestValModifier == null || entry.getValue()[mode] > biggestRemainingSecondaryRune.getValue()[mode]) {
                        biggestValModifier = entry;
                    }
                }
                r.getModifiers().add(biggestValModifier.getKey());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (!r.verify()) {
            log.error("Failed to verify the runepage.");
            return null;
        }
        return r;

    }

    @Override
    public void getRunesForChampion(Champion champion, GameMode mode, ObservableList<RunePage> pages) {
        downloadRunes(champion, pages);
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

    private static class DisplayConverted {
        private Map<Rune, int[]> rune1;
        private Map<Rune, int[]> rune2;
        private Map<Modifier, int[]> rune3;
        private Map<Modifier, int[]> rune4;
        private Map<Modifier, int[]> rune5;
    }


}
