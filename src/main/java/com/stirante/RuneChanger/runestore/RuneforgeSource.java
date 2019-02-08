package com.stirante.RuneChanger.runestore;

import com.google.gson.Gson;
import com.stirante.RuneChanger.model.*;
import com.stirante.RuneChanger.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RuneforgeSource implements RuneSource {

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies all runes from runeforge.gg
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {
            Champion.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
        RuneforgeSource src = new RuneforgeSource();
        for (Champion champion : Champion.values()) {
            System.out.println(champion.getName() + ":");
            List<RunePage> runes = src.getForChampion(champion);
            for (RunePage rune : runes) {
                if (!rune.verify()) {
                    System.out.println("\t\tBAD: " + rune.getUrl());
                }
            }
        }
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
            r.setUrl(url);
            //get rune page name
            r.setName(champion.getName() + ":" + StringUtils.fixString(parse.select("h2.loadout-title").text()));
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
            return r;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            RunePage runePage = new RunePage();
            runePage.setUrl(url);
            return runePage;
        }
    }

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @return list of rune pages
     */
    public List<RunePage> getForChampion(Champion champion) {
        if (pagesCache.containsKey(champion)) {
            return pagesCache.get(champion);
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
                if (loadout.loadout_champion_name.equalsIgnoreCase(champion.getName()) ||
                        loadout.loadout_champion_name.equalsIgnoreCase(champion.getAlias())) {
                    result.add(getRunes(champion, loadout.loadout_url));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        pagesCache.put(champion, result);
        return result;
    }

    private static class Loadout {
        private int loadout_id;
        private String loadout_url;
        private String loadout_champion_name;
        private String loadout_position_name;
        private Object loadout_champion_free;
        private String loadout_champion_grid;
        private String loadout_champion_centered;
        private String loadout_champion_centered_cdn;
        private String loadout_primary;
        private String loadout_keystone;
        private String loadout_playstyle;
        private Object loadout_player;
        private Object loadout_player_headshot;
    }

}
