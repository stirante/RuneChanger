package com.stirante.runechanger.runestore;

import com.google.gson.Gson;
import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.util.StringUtils;
import generated.Position;
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
            log.info("Champion source name: " + champion.getName());
            List<RunePage> runes = src.getForChampion(champion);
            for (RunePage rune : runes) {
                if (!rune.verify()) {
                    log.error("Bad rune source: " + rune.getSource());
                }
            }
            if (runes.size() == 0) {
                log.error("Bad rune source, reason: EMPTY");
            }
        }
    }

    public Position getPositionForChampion(Champion champion) {
        for (Loadout loadout : cache) {
            if (loadout.loadout_champion_name.equalsIgnoreCase(champion.getName()) ||
                    loadout.loadout_champion_name.equalsIgnoreCase(champion.getAlias()) ||
                    loadout.loadout_champion_name.equalsIgnoreCase(champion.getInternalName())) {
                switch (loadout.loadout_position_name) {
                    case "support":
                        return Position.UTILITY;
                    case "middle":
                        return Position.MIDDLE;
                    case "bottom":
                        return Position.BOTTOM;
                    case "top":
                        return Position.TOP;
                    case "jungle":
                        return Position.JUNGLE;
                    default:
                        log.warn("Unknown position name: " + loadout.loadout_position_name);
                        return Position.UNSELECTED;
                }
            }
        }
        log.warn("Champion not found: " + champion.getName());
        return Position.UNSELECTED;
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
            return r;
        } catch (IOException e) {
            log.error(e.getMessage());
            RunePage runePage = new RunePage();
            runePage.setSource(url);
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
                        loadout.loadout_champion_name.equalsIgnoreCase(champion.getAlias()) ||
                        loadout.loadout_champion_name.equalsIgnoreCase(champion.getInternalName())) {
                    result.add(getRunes(champion, loadout.loadout_url));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        pagesCache.put(champion, result);
        return result;
    }

    @Override
    public String getSourceName() {
        return "RuneForge.gg";
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
