package com.stirante.RuneChanger.model;

import com.google.gson.Gson;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.util.PathUtils;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Champion {

    private static File portraitsDir = new File(PathUtils.getAssetsDir(), "champions");
    private static List<Champion> values = new ArrayList<>(256);
    private static final AtomicBoolean IMAGES_READY = new AtomicBoolean(false);

    static {
        portraitsDir.mkdirs();
    }

    private final int id;
    private final String internalName;
    private final String name;
    private final String alias;
    private final String url;
    private java.awt.Image image;

    private Champion(int id, String internalName, String name, String alias, String url) {
        this.id = id;
        this.internalName = internalName;
        this.name = name;
        this.alias = alias;
        this.url = url;
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
                    champion.name.replaceAll(" ", ""), "https://cdn.communitydragon.org/" + patch + "/champion" +
                    "/" + champion.key + "/tile"));
        }
        new DownloadThread().start();
    }

    /**
     * Returns InputStream from provided url
     *
     * @param url url
     * @return InputStream from provided url
     */
    private static InputStream getUrl(String url) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
        return urlConnection.getInputStream();
    }

    /**
     * Returns image with champion's portrait
     */
    public java.awt.Image getPortrait() {
        return image;
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
     * Returns whether all images are downloaded and loaded into memory.
     */
    public static boolean areImagesReady() {
        return IMAGES_READY.get();
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Champion && ((Champion) obj).id == id;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name, id);
    }

    private static class DownloadThread extends Thread {

        @Override
        public void run() {
            for (Champion value : values) {
                File f = new File(portraitsDir, value.id + ".jpg");
                if (!f.exists()) {
                    try {
                        log.debug("Downloading portrait for " + value.getName());
                        HttpURLConnection conn = (HttpURLConnection) new URL(value.url).openConnection();
                        conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(f);
                        byte[] dataBuffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                            fileOutputStream.write(dataBuffer, 0, bytesRead);
                        }
                        in.close();
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    value.image = ImageIO.read(f);
                    value.image = value.image.getScaledInstance(70, 70, java.awt.Image.SCALE_SMOOTH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            log.info("Champions initialized");
            IMAGES_READY.set(true);
        }
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
