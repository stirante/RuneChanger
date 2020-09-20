package com.stirante.runechanger.sourcestore.impl;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.SyncingListWrapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;


// ._5bxv5t > div.desktop > div._xtoaop._4pvjjd > a > table > tbody > tr:nth-of-type(1) > td > h1
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
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {

        downloadRunes(data, pages);
    }

    public void downloadRunes(GameData data, SyncingListWrapper<RunePage> pages) {
        ArrayList<String> runeLanes = getAvailableRuneLanes(data.getChampion());
        for(String lane : Objects.requireNonNull(runeLanes)) {
            final String requestURL = CHAMPION_URL
                                        .replace("%MODE%", "5v5")
                                        .replace("%CHAMPION%", data.getChampion().getInternalName())
                                        .replace("%LANE%", lane.toLowerCase());
            try {
                Document webPage = Jsoup.parse(new URL(requestURL), 10000);
                RunePage r = extractRunes(webPage);
                r.setChampion(data.getChampion());
                r.setName(lane.substring(0, 1) + lane.toLowerCase().substring(1));
                r.setSource(requestURL);
                r.setSourceName("Metasrc");
                pages.add(r);
            } catch (Exception e) {
                log.warn("Error occured while getting Metasrc rune data for lane: " + lane);
                e.printStackTrace();
            }


        }

        String specialGamemodeURL = CHAMPION_URL
                .replace("%CHAMPION%", data.getChampion().getInternalName())
                .replace("%LANE%", "");
        // GETTING RUNES FOR SPECIAL GAMEMODES
        String gamemodeKey = getSpecialGamemodeKey(data);

        if(gamemodeKey != null) {
            final String requestURL = specialGamemodeURL.replace("%MODE%", gamemodeKey);
            try {
                Document webPage = Jsoup.parse(new URL(requestURL), 10000);
                RunePage r = extractRunes(webPage);
                r.setChampion(data.getChampion());
                r.setName(data.getGameMode().getName());
                r.setSource(requestURL);
                r.setSourceName("Metasrc");
                pages.add(r);
            } catch (Exception e) {
                log.warn("Failed to get Metasrc runedata for special gamemode: " + data.getGameMode().getName());
                e.printStackTrace();
            }
        }


    }

    private RunePage extractRunes(Document webPage) {
        RunePage r = new RunePage();
        Elements runes = webPage.select("div._lop72r:nth-of-type(2) > div._sjgjkw:nth-of-type(2) > div._sfh2p9 > div");
        Elements mainRunes = runes.get(0).select("div._hmag7l > div._xdda66 > div._q8ue62");
        Elements secondaryRunes = runes.get(1).select("div._hmag7l > div._xdda66 > div._q8ue62");
        r.setMainStyle(Style.getByName(mainRunes.get(0).text()));
        r.setSubStyle(Style.getByName(secondaryRunes.get(0).text()));


        for(int i = 1; i < 5; i++) {
            r.getRunes().add(Rune.getByName(mainRunes.get(i).text()));
        }

        for(int i = 1; i < 3; i++) {
            r.getRunes().add(Rune.getByName(secondaryRunes.get(i).text()));
        }

        for(int i = 3; i < 6; i++) {
            r.getModifiers().add(convertModifier(secondaryRunes.get(i).text()));
        }

        return r;

    }

    private ArrayList<String> getAvailableRuneLanes(Champion champion) {
        final String minimalURL = CHAMPION_URL
                                    .replace("%MODE%", "5v5")
                                    .replace("%CHAMPION%", champion.getInternalName())
                                    .replace("%LANE%", "");
        ArrayList<String> availableRoles = new ArrayList<>();
        try {
            Document webPage = Jsoup.parse(new URL(minimalURL), 10000);
            Elements rolesElements = webPage.select("._5bxv5t > div.desktop > div._xtoaop._4pvjjd > a > table > tbody > tr:nth-of-type(1) > td > h1");
            for(Element role : rolesElements) {
                availableRoles.add(role.text());
            }
        } catch (IOException e) {
            log.warn("Error occured while getting Metasrc available lane data.");
            e.printStackTrace();
        }
        return availableRoles;
    }

    private Modifier convertModifier(String modifier) {
        switch(modifier.toLowerCase()) {
            case "adaptive":
                return Modifier.RUNE_5008;
            case "armor":
                return Modifier.RUNE_5002;
            case "magicres":
                return Modifier.RUNE_5003;
            case "attackspeed":
                return Modifier.RUNE_5005;
            case "cdrscaling":
                return Modifier.RUNE_5007;
            case "healthscaling":
                return Modifier.RUNE_5001;
            default:
                return null;
        }
    }

    private String getSpecialGamemodeKey(GameData data) {
        switch(data.getGameMode()) {
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
            default:
                return null;
        }
    }
}
