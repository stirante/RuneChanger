package com.stirante.RuneChanger.crawler;

import com.google.gson.Gson;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.Rune;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.model.Style;
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
import java.util.List;

public class RuneforgeSource implements RuneSource {

    private static final String URL_ADDRESS = "https://d181w3hxxigzvh.cloudfront.net/all-loadouts-data.json";
    private static final int TIMEOUT = 10000;

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
     * Gets rune page from specified url
     *
     * @param url url
     * @return rune page
     */
    private RunePage getRunes(String url) {
        try {
            //get web page
            Document parse = Jsoup.parse(new URL(url), TIMEOUT);
            RunePage r = new RunePage();
            r.setUrl(url);
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
            return r;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets list of rune pages for champion
     *
     * @param champion champion
     * @return list of rune pages
     */
    public List<RunePage> getForChampion(Champion champion) {
        ArrayList<RunePage> result = new ArrayList<>();
        try {
            if (cache == null || cache.length == 0) {
                HttpURLConnection conn = (HttpURLConnection) new URL(URL_ADDRESS).openConnection();
                conn.connect();
                cache = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), Loadout[].class);
                conn.getInputStream().close();
            }
            for (Loadout loadout : cache) {
                if (loadout.getChampionName().equalsIgnoreCase(champion.getName()) || loadout.getChampionName().equalsIgnoreCase(champion.getAlias()))
                    result.add(getRunes(loadout.getURL()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Verifies all runes from runeforge.gg
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        RuneforgeSource src = new RuneforgeSource();
        for (Champion champion : Champion.values()) {
            List<RunePage> runes = src.getForChampion(champion);
            for (RunePage rune : runes) {
                if (!rune.verify()) System.out.println(rune.getUrl());
            }
        }
    }

    public static class Loadout {
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

        public int getId() {
            return loadout_id;
        }

        public void setId(int value) {
            this.loadout_id = value;
        }

        public String getURL() {
            return loadout_url;
        }

        public void setURL(String value) {
            this.loadout_url = value;
        }

        public String getChampionName() {
            return loadout_champion_name;
        }

        public void setChampionName(String value) {
            this.loadout_champion_name = value;
        }

        public String getPositionName() {
            return loadout_position_name;
        }

        public void setPositionName(String value) {
            this.loadout_position_name = value;
        }

        public Object getChampionFree() {
            return loadout_champion_free;
        }

        public void setChampionFree(Object value) {
            this.loadout_champion_free = value;
        }

        public String getChampionGrid() {
            return loadout_champion_grid;
        }

        public void setChampionGrid(String value) {
            this.loadout_champion_grid = value;
        }

        public String getChampionCentered() {
            return loadout_champion_centered;
        }

        public void setChampionCentered(String value) {
            this.loadout_champion_centered = value;
        }

        public String getChampionCenteredCdn() {
            return loadout_champion_centered_cdn;
        }

        public void setChampionCenteredCdn(String value) {
            this.loadout_champion_centered_cdn = value;
        }

        public String getPrimary() {
            return loadout_primary;
        }

        public void setPrimary(String value) {
            this.loadout_primary = value;
        }

        public String getKeystone() {
            return loadout_keystone;
        }

        public void setKeystone(String value) {
            this.loadout_keystone = value;
        }

        public String getPlaystyle() {
            return loadout_playstyle;
        }

        public void setPlaystyle(String value) {
            this.loadout_playstyle = value;
        }

        public Object getPlayer() {
            return loadout_player;
        }

        public void setPlayer(Object value) {
            this.loadout_player = value;
        }

        public Object getPlayerHeadshot() {
            return loadout_player_headshot;
        }

        public void setPlayerHeadshot(Object value) {
            this.loadout_player_headshot = value;
        }
    }

}
