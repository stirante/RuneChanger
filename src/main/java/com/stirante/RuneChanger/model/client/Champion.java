package com.stirante.RuneChanger.model.client;

import com.google.gson.Gson;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.runestore.RuneStore;
import com.stirante.RuneChanger.runestore.RuneforgeSource;
import com.stirante.RuneChanger.util.PathUtils;
import com.stirante.RuneChanger.util.StringUtils;
import generated.Position;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Champion {
    private static final Logger log = LoggerFactory.getLogger(Champion.class);

    private static File portraitsDir = new File(PathUtils.getAssetsDir(), "champions");
    private static final List<Champion> values = new ArrayList<>(256);
    private static final AtomicBoolean IMAGES_READY = new AtomicBoolean(false);
    private static final List<Runnable> imagesReadyEvenListeners = new ArrayList<>();

    static {
        portraitsDir.mkdirs();
    }

    private final int id;
    private final String internalName;
    private final String name;
    private final String alias;
    private final String url;
    private transient java.awt.Image image;
    private String pickQuote = "";
    private Position position;

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
        return List.copyOf(values);
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
        File cache = new File(portraitsDir, "champions.json");
        if (cache.exists()) {
            try {
                offlineInit(cache);
            } catch (Exception e) {
                //Cache failed, try online init
                e.printStackTrace();
                onlineInit(cache);
                return;
            }
            //Do online init anyways, but async, so it won't hold back GUI
            new Thread(() -> {
                try {
                    onlineInit(cache);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        else {
            //We don't have any cache for champions, so we get all champions on the main thread since champion list is required for GUI to work
            onlineInit(cache);
        }
    }

    private static void offlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        try (FileReader fileReader = new FileReader(cache)) {
            Champion[] champions = gson.fromJson(fileReader, Champion[].class);
            synchronized (Champion.values) {
                Champion.values.clear();
                Champion.values.addAll(Arrays.asList(champions));
            }
        }
    }

    private static void onlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        String patch;
        try (InputStream in = getUrl("https://ddragon.leagueoflegends.com/api/versions.json")) {
            String[] strings = gson.fromJson(new InputStreamReader(in), String[].class);
            patch = strings[0];
        }
        ChampionList champions;
        try (InputStream in = getUrl("http://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json")) {
            champions = gson.fromJson(new InputStreamReader(in), ChampionList.class);
        }
        List<ChampionDTO> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));

        synchronized (Champion.values) {
            Champion.values.clear();
            for (ChampionDTO champion : values) {
                Champion.values.add(new Champion(Integer.parseInt(champion.key), champion.id, champion.name,
                        champion.name.replaceAll(" ", ""), "https://cdn.communitydragon.org/" + patch + "/champion" +
                        "/" + champion.key));
            }
        }

        //Save json with champions to file for faster initialization next time
        try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
            out.print(gson.toJson(Champion.values()));
            out.flush();
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
     * Get champion pick quote
     *
     * @return alias
     */
    public String getPickQuote() {
        return pickQuote;
    }

    /**
     * Returns whether all images are downloaded and loaded into memory.
     */
    public static boolean areImagesReady() {
        return IMAGES_READY.get();
    }

    public static void addImagesReadyListener(Runnable r) {
        imagesReadyEvenListeners.add(r);
    }

    public BufferedImage getSplashArt() {
        try {
            return ImageIO.read(new File(portraitsDir, id + "_full.jpg"));
        } catch (IOException e) {
            return null;
        }
    }

    public Position getPosition() {
        return position;
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
            for (Champion champion : values) {
                // Checking and downloading portrait
                File f = new File(portraitsDir, champion.id + ".jpg");
                checkAndDownload(f,
                        champion.url + "/tile",
                        "v1/champion-tiles/" + champion.getId() + "/" + champion.getId() + "000.jpg");
                // Loading a scaled version of portrait
                try {
                    champion.image = ImageIO.read(f);
                    champion.image = champion.image.getScaledInstance(70, 70, java.awt.Image.SCALE_SMOOTH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Checking and downloading splash art
                f = new File(portraitsDir, champion.id + "_full.jpg");
                checkAndDownload(f,
                        champion.url + "/splash-art/centered",
                        "v1/champion-splashes/" + champion.getId() + "/" + champion.getId() + "000.jpg");
                // Getting pick quote for champion
                if (champion.pickQuote.isEmpty()) {
                    getQuoteForChampion(champion);
                }
                RuneforgeSource source = RuneStore.getSource(RuneforgeSource.class);
                if (source != null) {
                    champion.position = source.getPositionForChampion(champion);
                }
            }

            try {
                Gson gson = new Gson();
                File cache = new File(portraitsDir, "champions.json");
                //Save json with champions to file for faster initialization next time (this time with pick quotes)
                try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
                    out.print(gson.toJson(Champion.values()));
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            log.info("Champions initialized");
            IMAGES_READY.set(true);
            for (Runnable r : imagesReadyEvenListeners) {
                try {
                    r.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            imagesReadyEvenListeners.clear();
        }

        private void getQuoteForChampion(Champion champion) {
            try {
                Document doc = Jsoup.parse(new URL("https://leagueoflegends.fandom.com/wiki/" +
                        URLEncoder.encode(champion.getName().replaceAll(" ", "_"), StandardCharsets.UTF_8) +
                        "/Quotes"), 60000);
                Elements select = doc.select("#mw-content-text").first().children();
                boolean isPick = false;
                for (Element element : select) {
                    if (element.tagName().equalsIgnoreCase("dl") && element.text().equalsIgnoreCase("Pick")) {
                        isPick = true;
                        continue;
                    }
                    if (isPick) {
                        champion.pickQuote = StringUtils.fixString(element.text()).replaceAll("\"", "");
                        break;
                    }
                }
                if (!isPick) {
                    Elements elements = doc.select("#mw-content-text > .tabber > .tabbertab");
                    if (elements != null && elements.first() != null) {
                        select = elements.first().children();
                        for (Element element : select) {
                            if (element.tagName().equalsIgnoreCase("dl") &&
                                    element.text().equalsIgnoreCase("Pick")) {
                                isPick = true;
                                continue;
                            }
                            if (isPick) {
                                champion.pickQuote = StringUtils.fixString(element.select("i").first().text())
                                        .replaceAll("\"", "");
                                break;
                            }
                        }
                    }
                    if (!isPick) {
                        champion.pickQuote = champion.name + " laughs.";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void checkAndDownload(File f, String url, String lcuPath) {
            if (!f.exists()) {
                try {
                    BufferedInputStream in;
                    if (RuneChanger.getInstance() == null || RuneChanger.getInstance().getApi() == null ||
                            !RuneChanger.getInstance().getApi().isConnected()) {
                        log.info("Downloading " + url);
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                        in = new BufferedInputStream(conn.getInputStream());
                    }
                    else {
                        log.info("Getting LCU asset " + lcuPath);
                        in = new BufferedInputStream(RuneChanger.getInstance()
                                .getApi()
                                .getAsset("lol-game-data", lcuPath));
                    }
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
        }

    }

    public static class ChampionDTO {
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

    public static class ChampionList {
        public String type;
        public String format;
        public String version;
        public HashMap<String, ChampionDTO> data;
    }

    public static class Image {
        public String full;
        public String sprite;
        public String group;
        public Integer x;
        public Integer y;
        public Integer w;
        public Integer h;
    }

    public static class Info {
        public Double attack;
        public Double defense;
        public Double magic;
        public Double difficulty;
    }

    public static class Stats {
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
