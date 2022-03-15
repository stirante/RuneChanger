package com.stirante.runechanger.sourcestore.impl;

import com.stirante.runechanger.api.*;
import com.stirante.runechanger.model.client.GameData;
import com.stirante.runechanger.model.client.GameMode;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.utils.SyncingListWrapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


public class MetasrcSource implements RuneSource {
    final String CHAMPION_URL = "https://metasrc.com/%MODE%/champion/%CHAMPION%/%LANE%";
    private static final Logger log = LoggerFactory.getLogger(MetasrcSource.class);

    @Override
    public String getSourceName() {
        return "Metasrc";
    }

    @Override
    public String getSourceKey() {
        return "metasrc.com";
    }

    @Override
    public void getRunesForGame(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        downloadRunes(data, pages);
    }

    public void downloadRunes(GameData data, SyncingListWrapper<ChampionBuild> pages) {
        // This makes sure that the runes wont get duplicated when getting them from GUI
        if (data.getGameMode() == GameMode.CLASSIC || data.getContext() == GameData.Context.CHAMPION_SELECT) {
            ArrayList<String> runeLanes = getAvailableRuneLanes(data.getChampion());
            for (String lane : Objects.requireNonNull(runeLanes)) {
                final String requestURL = CHAMPION_URL
                        .replace("%MODE%", "5v5")
                        .replace("%CHAMPION%", data.getChampion().getInternalName().toLowerCase(Locale.ROOT))
                        .replace("%LANE%", lane.toLowerCase());
                try {
                    Document webPage = Jsoup.parse(new URL(requestURL), 10000);
                    RunePage r = extractRunes(webPage);
                    if (r == null) {
                        continue;
                    }
                    r.setChampion(data.getChampion());
                    r.setName(lane.charAt(0) + lane.toLowerCase().substring(1));
                    r.setSource(requestURL);
                    r.setSourceName("Metasrc");
                    pages.add(ChampionBuild.builder(r).withSpells(extractSpells(webPage)).create());
                } catch (Exception e) {
                    log.warn("Error occured while getting Metasrc rune data for lane: " + lane);
                    e.printStackTrace();
                }
            }
        }

        String specialGamemodeURL = CHAMPION_URL
                .replace("%CHAMPION%", data.getChampion().getInternalName().toLowerCase(Locale.ROOT))
                .replace("%LANE%", "");
        // GETTING RUNES FOR SPECIAL GAMEMODES
        String gamemodeKey = getSpecialGamemodeKey(data);

        if (gamemodeKey != null) {
            final String requestURL = specialGamemodeURL.replace("%MODE%", gamemodeKey);
            try {
                Document webPage = Jsoup.parse(new URL(requestURL), 10000);
                RunePage r = extractRunes(webPage);
                if (r == null) {
                    return;
                }
                r.setChampion(data.getChampion());
                r.setName(data.getGameMode().getName());
                r.setSource(requestURL);
                r.setSourceName("Metasrc");
                pages.add(ChampionBuild.builder(r).create());
            } catch (Exception e) {
                log.warn("Failed to get Metasrc runedata for special gamemode: " + data.getGameMode().getName());
                e.printStackTrace();
            }
        }


    }

    private List<SummonerSpell> extractSpells(Document webPage) {
        // ._qngo9y > div:nth-child(3) > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1)
        Elements spells =
                webPage.select("._qngo9y > div:nth-child(3) > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div > ._hmag7l");
        if (spells.size() == 0) {
            return Collections.emptyList();
        }
        return spells.stream()
                .map(element -> SummonerSpell.getByKey(Integer.parseInt(element.attr("data-tooltip")
                        .replace("spell-", ""))))
                .collect(Collectors.toList());
    }

    private RunePage extractRunes(Document webPage) {
        RunePage r = new RunePage();
        Elements runes =
                webPage.select("div._lop72r:nth-of-type(3) > div._sh98mb:nth-of-type(2) div._sfh2p9 > div > div");
        if (runes.size() == 0) {
            return null;
        }
        List<Integer> mainRunes = runes.get(0)
                .select("div._hmag7l")
                .stream()
                .map(element -> Integer.parseInt(element.attr("data-tooltip").replace("perk-", "")))
                .collect(Collectors.toList());
        List<Integer> secondaryRunes = runes.get(1)
                .select("div._hmag7l")
                .stream()
                .map(element -> Integer.parseInt(element.attr("data-tooltip").replace("perk-", "")))
                .collect(Collectors.toList());
        r.setMainStyle(Style.getById(mainRunes.get(0)));
        r.setSubStyle(Style.getById(secondaryRunes.get(0)));


        for (int i = 1; i < 5; i++) {
            r.getRunes().add(Rune.getById(mainRunes.get(i)));
        }

        for (int i = 1; i < 3; i++) {
            r.getRunes().add(Rune.getById(secondaryRunes.get(i)));
        }

        for (int i = 3; i < 6; i++) {
            r.getModifiers().add(Modifier.getById(secondaryRunes.get(i)));
        }

        return r;

    }

    private ArrayList<String> getAvailableRuneLanes(Champion champion) {
        final String minimalURL = CHAMPION_URL
                .replace("%MODE%", "5v5")
                .replace("%CHAMPION%", champion.getInternalName().toLowerCase(Locale.ROOT))
                .replace("%LANE%", "");
        ArrayList<String> availableRoles = new ArrayList<>();
        try {
            Document webPage = Jsoup.parse(new URL(minimalURL), 10000);
            // Get possible roles
            Elements rolesElements =
                    webPage.select("div._tf4dk4 > a .desktop");
            for (Element role : rolesElements) {
                availableRoles.add(role.text());
            }
            // Add main one
            rolesElements =
                    webPage.select("div._tf4dk4._q9sxji .desktop");
            for (Element role : rolesElements) {
                availableRoles.add(role.text());
            }
        } catch (IOException e) {
            log.warn("Error occurred while getting Metasrc available lane data.");
            e.printStackTrace();
        }
        return availableRoles;
    }

    private String getSpecialGamemodeKey(GameData data) {
        switch (data.getGameMode()) {
            case ONEFORALL:
                return "ofa";
            case URF:
                return "urf";
            case ARAM:
                return "aram";
            case KINGPORO:
                return "poro";
            case NEXUSBLITZ:
                return "blitz";
            case ULTBOOK:
                return "ultbook";
            default:
                return null;
        }
    }

    @Override
    public GameMode[] getSupportedGameModes() {
        return new GameMode[]{GameMode.CLASSIC, GameMode.ONEFORALL, GameMode.URF, GameMode.ARAM, GameMode.KINGPORO, GameMode.NEXUSBLITZ, GameMode.ULTBOOK};
    }

    public static void main(String[] args) throws IOException {
        SourceStore.testSource(new MetasrcSource(), GameMode.CLASSIC);
    }
}
