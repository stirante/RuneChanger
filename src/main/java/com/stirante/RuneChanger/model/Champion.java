package com.stirante.RuneChanger.model;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Champion {

    private static List<Champion> values = new ArrayList<>(256);

    private final int id;
    private final String internalName;
    private final String name;
    private final String alias;

    Champion(int id, String internalName, String name, String alias) {
        this.id = id;
        this.internalName = internalName;
        this.name = name;
        this.alias = alias;
    }

    /**
     * Return all champions
     *
     * @return unmodifiable list of all champions
     */
    public static List<Champion> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Get champion id
     *
     * @return champion id
     */
    public int getId() {
        return id;
    }

    /**
     * Get riot internal champion name
     *
     * @return internal champion name
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * Get champion name
     *
     * @return champion name
     */
    public String getName() {
        return name;
    }

    /**
     * Get alternative champion name
     *
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get champion by id
     *
     * @param id id
     * @return champion
     */
    public static Champion getById(int id) {
        for (Champion champion : values) {
            if (champion.id == id) {
                return champion;
            }
        }
        return null;
    }

    /**
     * Get champion by name
     *
     * @param name name
     * @return champion
     */
    public static Champion getByName(String name) {
        for (Champion champion : values) {
            if (champion.name.equalsIgnoreCase(name) || champion.alias.equalsIgnoreCase(name) ||
                    champion.internalName.equalsIgnoreCase(name)) {
                return champion;
            }
        }
        return null;
    }

    /**
     * Initializes all champions in game
     */
    public static void init() throws IOException {
        Gson gson = new Gson();
        InputStream in = getUrl("https://ddragon.leagueoflegends.com/api/versions.json");
        String[] strings = gson.fromJson(new InputStreamReader(in), String[].class);
        String patch = strings[0];
        in = getUrl("http://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json");
        ChampionList champions = gson.fromJson(new InputStreamReader(in), ChampionList.class);
        in.close();
        List<ChampionDTO> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));
        for (ChampionDTO champion : values) {
            Champion.values.add(new Champion(Integer.parseInt(champion.key), champion.id, champion.name,
                    champion.name.replaceAll(" ", "")));
        }
    }

    /**
     * Returns InputStream from provided url
     *
     * @param url url
     * @return InputStream from provided url
     */
    private static InputStream getUrl(String url) throws IOException {
        System.out.println(url);
        URL urlObject = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
        return urlConnection.getInputStream();
    }

    public class ChampionDTO {
        public String version;
        public String id;
        public String key;
        public String name;
        public String title;
        public String blurb;
        public Info info;
        public Image image;
        public List<String> tags = null;
        public String partype;
        public Stats stats;
    }

    public class ChampionList {
        public String type;
        public String format;
        public String version;
        public HashMap<String, ChampionDTO> data;
    }

    public class Image {
        public String full;
        public String sprite;
        public String group;
        public Integer x;
        public Integer y;
        public Integer w;
        public Integer h;
    }

    public class Info {
        public Double attack;
        public Double defense;
        public Double magic;
        public Double difficulty;
    }

    public class Stats {
        public Double hp;
        public Double hpperlevel;
        public Double mp;
        public Double mpperlevel;
        public Double movespeed;
        public Double armor;
        public Double armorperlevel;
        public Double spellblock;
        public Double spellblockperlevel;
        public Double attackrange;
        public Double hpregen;
        public Double hpregenperlevel;
        public Double mpregen;
        public Double mpregenperlevel;
        public Double crit;
        public Double critperlevel;
        public Double attackdamage;
        public Double attackdamageperlevel;
        public Double attackspeedperlevel;
        public Double attackspeed;
    }

}
