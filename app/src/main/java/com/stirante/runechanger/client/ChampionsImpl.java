package com.stirante.runechanger.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stirante.eventbus.EventBus;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.Champions;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.model.client.ChampionImpl;
import com.stirante.runechanger.model.client.Patch;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.sourcestore.impl.ChampionGGSource;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.util.PerformanceMonitor;
import com.stirante.runechanger.utils.AsyncTask;
import com.stirante.runechanger.utils.PathUtils;
import com.stirante.runechanger.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChampionsImpl implements Champions {

    private static final Logger log = LoggerFactory.getLogger(ChampionsImpl.class);
    private final List<Champion> values = new ArrayList<>(256);
    private final AtomicBoolean IMAGES_READY = new AtomicBoolean(false);
    private static final File portraitsDir = new File(PathUtils.getAssetsDir(), "champions");

    static {
        portraitsDir.mkdirs();
    }

    private final RuneChangerApi api;

    public ChampionsImpl(RuneChangerApi api) {
        this.api = api;
    }

    @Override
    public List<Champion> getChampions() {
        return List.copyOf(values);
    }

    @Override
    public Champion getById(int id) {
        for (Champion champion : values) {
            if (champion.getId() == id) {
                return champion;
            }
        }
        return null;
    }

    @Override
    public Champion getByName(String name) {
        return getByName(name, false);
    }

    @Override
    public Champion getByName(String name, boolean additionalChecks) {
        for (Champion champion : values) {
            if (champion.getName().equalsIgnoreCase(name) || champion.getAlias().equalsIgnoreCase(name) ||
                    champion.getInternalName().equalsIgnoreCase(name) ||
                    (additionalChecks && champion.getName().replaceAll("[^a-zA-Z]", "")
                            .equalsIgnoreCase(name.replaceAll("[^a-zA-Z]", "")))) {
                return champion;
            }
        }
        return null;
    }

    /**
     * Initializes all champions in game
     */
    public void init() throws IOException {
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.CHAMPIONS_INIT_START);
        File cache = new File(portraitsDir, "champions.json");
        if (cache.exists()) {
            try {
                offlineInit(cache);
            } catch (Exception e) {
                //Cache failed, try online init
                log.error("Exception occurred while performing offline init of champions", e);
                onlineInit(cache);
                return;
            }
            //Do online init anyways, but async, so it won't hold back GUI
            AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                try {
                    onlineInit(cache);
                } catch (IOException e) {
                    log.error("Exception occurred while performing online init of champions", e);
                }
            });
        }
        else {
            //We don't have any cache for champions, so we get all champions on the main thread since champion list is required for GUI to work
            onlineInit(cache);
        }
    }

    private void offlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        try (FileReader fileReader = new FileReader(cache)) {
            ChampionImpl[] champions = gson.fromJson(fileReader, ChampionImpl[].class);
            synchronized (values) {
                values.clear();
                values.addAll(Arrays.asList(champions));
            }
        }
    }

    private void onlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        ChampionList champions;
        try (InputStream in = getUrl("http://ddragon.leagueoflegends.com/cdn/" + Patch.getLatest().toFullString() +
                "/data/en_US/champion.json")) {
            champions = gson.fromJson(new InputStreamReader(in), ChampionList.class);
        }
        List<ChampionDTO> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));

        Patch patch = Patch.getLatest();
        List<Patch> latest = Patch.getLatest(5);
        for (Patch p : latest) {
            try {
                if (isOk("https://cdn.communitydragon.org/" + p.toFullString() + "/champion/Annie/square")) {
                    patch = p;
                    break;
                }
            } catch (IOException ignored) {
            }
        }

        synchronized (this.values) {
            boolean allExist = true;
            for (ChampionDTO champion : values) {
                ChampionImpl c = new ChampionImpl(Integer.parseInt(champion.key), champion.id, champion.name,
                        champion.name.replaceAll(" ", ""),
                        "https://cdn.communitydragon.org/" + patch.toFullString() + "/champion" +
                                "/" + champion.key);
                if (this.values.contains(c)) {
                    ChampionImpl champion1 =
                            (ChampionImpl) this.values.stream()
                                    .filter(champ -> champ.getId() == c.getId())
                                    .findFirst()
                                    .orElseThrow();
                    champion1.setName(c.getName());
                    champion1.setInternalName(c.getInternalName());
                    champion1.setAlias(c.getAlias());
                    champion1.setUrl(c.getUrl());
                }
                else {
                    this.values.add(c);
                }

                File f = new File(portraitsDir, c.getId() + ".jpg");
                if (!f.exists()) {
                    allExist = false;
                }
            }
            if (allExist) {
                log.info("Portraits ready");
                publishImagesReadyEvent();
            }
        }

        //Save json with champions to file for faster initialization next time
        try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
            out.print(gson.toJson(getChampions()));
            out.flush();
        }

        new DownloadThread().start();
    }

    private void publishImagesReadyEvent() {
        IMAGES_READY.set(true);
        try {
            EventBus.publish(IMAGES_READY_EVENT);
        } catch (Throwable t) {
            log.error("Exception occurred while publishing images ready event", t);
            AnalyticsUtil.addCrashReport(t, "Exception occurred while publishing images ready event", false);
        }
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

    private static boolean isOk(String url) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
        urlConnection.setRequestMethod("HEAD");
        int responseCode = urlConnection.getResponseCode();
        urlConnection.disconnect();
        return responseCode >= 200 && responseCode < 300;
    }

    /**
     * Returns whether all images are downloaded and loaded into memory.
     */
    @Override
    public boolean areImagesReady() {
        return IMAGES_READY.get();
    }

    private class DownloadThread extends Thread {

        @Override
        public void run() {
            Gson gson = new Gson();
            Map<String, Long> splashes = new HashMap<>();
            try (InputStream in = new URL("https://runechanger.stirante.com/splashes.json").openStream()) {
                splashes = gson.fromJson(new InputStreamReader(in), new TypeToken<Map<String, Long>>() {
                }.getType());
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
            portraitsDir.getParentFile().mkdirs();
            for (Champion champion : values) {
                // Checking and downloading portrait
                File f = new File(portraitsDir, champion.getId() + ".jpg");
                boolean force = splashes.containsKey(String.valueOf(champion.getId())) &&
                        f.exists() &&
                        splashes.get(String.valueOf(champion.getId())) > f.lastModified();
                checkAndDownload(f,
                        ((ChampionImpl) champion).getUrl() + "/tile",
                        "v1/champion-tiles/" + champion.getId() + "/" + champion.getId() + "000.jpg", force);
                // Checking and downloading splash art
                f = new File(portraitsDir, champion.getId() + "_full.jpg");
                force = splashes.containsKey(String.valueOf(champion.getId())) &&
                        f.exists() &&
                        splashes.get(String.valueOf(champion.getId())) > f.lastModified();
                checkAndDownload(f,
                        ((ChampionImpl) champion).getUrl() + "/splash-art/centered",
                        "v1/champion-splashes/" + champion.getId() + "/" + champion.getId() + "000.jpg", force);
                // Getting pick quote for champion
                if (champion.getPickQuote().isEmpty()) {
                    getQuoteForChampion(champion);
                }
                ChampionGGSource source = SourceStore.getSource(ChampionGGSource.class);
                if (source != null) {
                    ((ChampionImpl) champion).setPosition(source.getPositionForChampion(champion));
                }
            }

            try {
                File cache = new File(portraitsDir, "champions.json");
                //Save json with champions to file for faster initialization next time (this time with pick quotes)
                try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
                    out.print(gson.toJson(getChampions()));
                    out.flush();
                }
            } catch (IOException e) {
                log.error("Exception occurred while saving champions cache", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while saving champions cache", false);
            }

            log.info("Champions initialized");
            publishImagesReadyEvent();
            PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.CHAMPIONS_INIT_END);
        }

        private void getQuoteForChampion(Champion champion) {
            try {
                Document doc = Jsoup.parse(new URL("https://leagueoflegends.fandom.com/wiki/" +
                        // Special case for Nunu...
                        URLEncoder.encode(champion.getName()
                                .replaceAll(" ", "_")
                                .replaceAll("_&.+", ""), StandardCharsets.UTF_8) +
                        "/LoL/Audio"), 60000);
                Elements select = doc.select("#mw-content-text .mw-parser-output").first().children();
                boolean isPick = false;
                for (Element element : select) {
                    if (element.tagName().equalsIgnoreCase("dl") && element.text().equalsIgnoreCase("Pick")) {
                        isPick = true;
                        continue;
                    }
                    if (isPick) {
                        ((ChampionImpl) champion).setPickQuote(StringUtils.fixString(element.text())
                                .replaceAll("\"", ""));
                        break;
                    }
                }
                if (!isPick) {
                    Elements elements = doc.select("#mw-content-text > .tabber > .tabbertab");
                    if (elements.first() != null) {
                        select = elements.first().children();
                        for (Element element : select) {
                            if (element.tagName().equalsIgnoreCase("dl") &&
                                    element.text().equalsIgnoreCase("Pick")) {
                                isPick = true;
                                continue;
                            }
                            if (isPick) {
                                ((ChampionImpl) champion).setPickQuote(StringUtils.fixString(element.select("i")
                                                .first()
                                                .text())
                                        .replaceAll("\"", ""));
                                break;
                            }
                        }
                    }
                }
                if (!isPick || champion.getPickQuote() == null || champion.getPickQuote().isBlank()) {
                    ((ChampionImpl) champion).setPickQuote(champion.getName() + " laughs.");
                }
            } catch (IOException e) {
                log.error("Exception occurred while getting quote for champion " + champion.getPickQuote(), e);
            }
        }

        private void checkAndDownload(File f, String url, String lcuPath, boolean force) {
            if (!f.exists() || force) {
                if (force) {
                    log.info("Forcing download, because champion splash art changed");
                }
                try {
                    BufferedInputStream in;
                    if (api == null || api.getClientApi() == null ||
                            !api.getClientApi().isConnected()) {
                        log.info("Downloading " + url);
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                        in = new BufferedInputStream(conn.getInputStream());
                    }
                    else {
                        log.info("Getting LCU asset " + lcuPath);
                        in = new BufferedInputStream(api
                                .getClientApi()
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
                    log.error("Exception occurred while downloading asset", e);
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
