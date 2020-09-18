package com.stirante.runechanger.sourcestore.impl;

import com.google.gson.Gson;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.sourcestore.RuneSource;
import com.stirante.runechanger.util.SyncingListWrapper;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RuneforgeSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(RuneforgeSource.class);

    private static final String URL_ADDRESS = "https://runeforge.gg/all-loadouts-data.json";
    private static final int TIMEOUT = 10000;
    private static final HashMap<Champion, List<RunePage>> pagesCache = new HashMap<>();
    private static Loadout[] cache = null;

    public RuneforgeSource() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(URL_ADDRESS).openConnection();
            conn.connect();
            cache = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), Loadout[].class);
            conn.getInputStream().close();
        } catch (Exception e) {
            log.error("Exception occurred while getting RuneForge source data", e);
        }
    }

    @Override
    public String getSourceKey() {
        return "runeforge.gg";
    }

    /**
     * Gets rune page from specified url
     *
     * @param url url
     * @return rune page
     */
    private RunePage getRunes(Champion champion, String url) {
        try {
            //get web page
            Document parse = Jsoup.parse(new URL(url), TIMEOUT);
            RunePage r = new RunePage();
            r.setSource(url);
            r.setSourceName(getSourceName());
            //get rune page name
            r.setName(StringUtils.fixString(parse.select("h2.loadout-title").text()));
            Element style = parse.getElementsByClass("rune-path--primary").first();
            String styleName = style.getElementsByClass("rune-path--path").first().attr("data-content-title");
            r.setMainStyle(Style.getByName(styleName));
            Elements runes = style.getElementsByClass("rune-path--tree").first().getElementsByClass("rune-path--rune");
            for (Element rune : runes) {
                String attr = rune.attr("data-link-title");
                r.getRunes().add(Rune.getByName(attr));
            }
            style = parse.getElementsByClass("rune-path--secondary").first();
            styleName = style.getElementsByClass("rune-path--path").first().attr("data-content-title");
            r.setSubStyle(Style.getByName(styleName));
            runes = style.getElementsByClass("rune-path--tree").first().getElementsByClass("rune-path--rune");
            for (Element rune : runes) {
                String attr = rune.attr("data-link-title");
                r.getRunes().add(Rune.getByName(attr));
            }
            if (!parse.getElementsByClass("stat-shards").isEmpty()) {
                Elements tree = parse.getElementsByClass("stat-shards").first().getElementsByTag("li");

                for (Element element : tree) {
                    String text = element.getElementsByClass("rune-path--rune_description").first().text();
                    r.getModifiers().add(Modifier.getByName(text));
                }
            }
            r.setChampion(champion);
            if (!r.verify()) {
                return null;
            }
            return r;
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Gets list of rune pages for champion
     *
     * @param data game data
     * @return list of rune pages
     */
    public void getRunesForGame(GameData data, SyncingListWrapper<RunePage> pages) {
        if (pagesCache.containsKey(data.getChampion())) {
            pages.addAll(pagesCache.get(data.getChampion()));
            return;
        }
        ArrayList<RunePage> result = new ArrayList<>();
        try {
            if (cache == null || cache.length == 0) {
                HttpURLConnection conn = (HttpURLConnection) new URL(URL_ADDRESS).openConnection();
                conn.connect();
                cache = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), Loadout[].class);
                conn.getInputStream().close();
            }
            for (Loadout loadout : cache) {
                if (loadout.loadout_champion_name.equalsIgnoreCase(data.getChampion().getName()) ||
                        loadout.loadout_champion_name.equalsIgnoreCase(data.getChampion().getAlias()) ||
                        loadout.loadout_champion_name.equalsIgnoreCase(data.getChampion().getInternalName())) {
                    RunePage runes = getRunes(data.getChampion(), loadout.loadout_url);
                    if (runes != null) {
                        result.add(runes);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting RuneForge rune page for champion " + data.getChampion().getName(), e);
        }
        pagesCache.put(data.getChampion(), result);
        FxUtils.doOnFxThread(() -> pages.addAll(result));
    }

    @Override
    public String getSourceName() {
        return "RuneForge.gg";
    }

    private static class Loadout {
        public String loadout_url;
        public String loadout_champion_name;
    }

}
