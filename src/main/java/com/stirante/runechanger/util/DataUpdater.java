package com.stirante.runechanger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.runechanger.model.client.Rune;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Internal class for generating enums and resources
 */
public class DataUpdater {
    private static final Logger log = LoggerFactory.getLogger(DataUpdater.class);

    private static final String RUNE_ENUM_PREFIX = "package com.stirante.runechanger.model.client;\n" +
            "\n" +
            "import org.slf4j.Logger;\n" +
            "import org.slf4j.LoggerFactory;\n" +
            "\n" +
            "import javax.imageio.ImageIO;\n" +
            "import java.awt.image.BufferedImage;\n" +
            "import java.io.IOException;\n" +
            "\n" +
            "public enum Rune {\n";
    private static final String RUNE_ENUM_POSTFIX = "\n" +
            "\n" +
            "    private static final Logger log = LoggerFactory.getLogger(Rune.class);\n" +
            "    private final int id;\n" +
            "    private final Style style;\n" +
            "    private final int slot;\n" +
            "    private final String name;\n" +
            "    private BufferedImage image;\n" +
            "\n" +
            "    Rune(int id, int styleId, int slot, String name) {\n" +
            "        this.id = id;\n" +
            "        style = Style.getById(styleId);\n" +
            "        this.slot = slot;\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune by name\n" +
            "     *\n" +
            "     * @param name rune name\n" +
            "     * @return rune\n" +
            "     */\n" +
            "    public static Rune getByName(String name) {\n" +
            "        for (Rune rune : values()) {\n" +
            "            if (rune.name.equalsIgnoreCase(name) || rune.name.replaceAll(\"'\", \"’\").equalsIgnoreCase(name)) {\n" +
            "                return rune;\n" +
            "            }\n" +
            "        }\n" +
            "        log.error(\"Rune name: \" + name + \" not found\");\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune by id\n" +
            "     *\n" +
            "     * @param id rune id\n" +
            "     * @return rune\n" +
            "     */\n" +
            "    public static Rune getById(int id) {\n" +
            "        for (Rune rune : values()) {\n" +
            "            if (rune.id == id) {\n" +
            "                return rune;\n" +
            "            }\n" +
            "        }\n" +
            "        log.error(\"Rune id: \" + id + \" not found\");\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune id\n" +
            "     *\n" +
            "     * @return rune id\n" +
            "     */\n" +
            "    public int getId() {\n" +
            "        return id;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune name\n" +
            "     *\n" +
            "     * @return rune name\n" +
            "     */\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get style this rune belongs to\n" +
            "     *\n" +
            "     * @return style\n" +
            "     */\n" +
            "    public Style getStyle() {\n" +
            "        return style;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get slot where this rune can be placed\n" +
            "     *\n" +
            "     * @return slot\n" +
            "     */\n" +
            "    public int getSlot() {\n" +
            "        return slot;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune image. Works only for keystones\n" +
            "     *\n" +
            "     * @return image\n" +
            "     */\n" +
            "    public BufferedImage getImage() {\n" +
            "        if (getSlot() != 0) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        if (image == null) {\n" +
            "            try {\n" +
            "                image = ImageIO.read(getClass().getResourceAsStream(\"/runes/\" + getId() + \".png\"));\n" +
            "            } catch (IOException e) {\n" +
            "                log.error(\"Exception occurred while reading rune icon\", e);\n" +
            "            }\n" +
            "        }\n" +
            "        return image;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String toString() {\n" +
            "        return name() + \"(\" + name + \")\";\n" +
            "    }\n" +
            "}\n";


    private static InputStream getEndpoint(String endpoint) throws IOException {
        log.debug("Endpoint: " + endpoint);
        URL url = new URL(endpoint);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        return urlConnection.getInputStream();
    }

    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().create();
        String patch = getLatestPatch(gson);
        generateRunes(gson, patch);
        downloadImages();
    }

    private static String getLatestPatch(Gson gson) throws IOException {
        InputStream in = getEndpoint("https://ddragon.leagueoflegends.com/api/versions.json");
        String[] strings = gson.fromJson(new InputStreamReader(in), String[].class);
        return strings[0];
    }

    private static void generateRunes(Gson gson, String patch) throws IOException {
        InputStream in =
                getEndpoint("http://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/runesReforged.json");
        StringBuilder sb = new StringBuilder();
        ReforgedRunePathDto[] runes = gson.fromJson(new InputStreamReader(in), ReforgedRunePathDto[].class);
        in.close();
        List<ReforgedRuneDto> runes1 = new ArrayList<>();
        for (ReforgedRunePathDto rune : runes) {
            List<ReforgedRuneSlotDto> slots = rune.slots;
            for (int i = 0; i < slots.size(); i++) {
                ReforgedRuneSlotDto slot = slots.get(i);
                for (ReforgedRuneDto runeDto : slot.runes) {
                    runeDto.slot = i;
                    runeDto.runePathId = rune.id;
                }
                runes1.addAll(slot.runes);
            }
        }
        runes1.sort(Comparator.comparingInt(o -> o.id));
        for (int i = 0; i < runes1.size(); i++) {
            ReforgedRuneDto rune = runes1.get(i);
            sb.append("    RUNE_")
                    .append(rune.id)
                    .append("(")
                    .append(rune.id)
                    .append(", ")
                    .append(rune.runePathId)
                    .append(", ")
                    .append(rune.slot)
                    .append(", \"")
                    .append(rune.name)
                    .append("\")");
            if (i == runes1.size() - 1) {
                sb.append(";\n");
            }
            else {
                sb.append(",\n");
            }
        }
        try {
            FileWriter writer = new FileWriter(new File("src/main/java/com/stirante/RuneChanger/model/client/Rune.java"));
            writer.write(RUNE_ENUM_PREFIX + "    //Generated on " +
                    SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH).format(new Date()) + "\n" + sb.toString() +
                    RUNE_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Exception occurred while generating Rune enum", e);
        }
    }

    private static void downloadImages() {
        HashMap<Rune, String> replacements = new HashMap<>();
        replacements.put(Rune.RUNE_8439, "veteranaftershock");
        replacements.put(Rune.RUNE_8358, "masterkey");
        for (Rune rune : Rune.values()) {
            if (rune.getSlot() == 0) {
                String internalName = rune.getName().toLowerCase().replaceAll(" ", "");
                if (replacements.containsKey(rune)) {
                    internalName = replacements.get(rune);
                }
                try {
                    String url =
                            "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/perk-images/styles/";
                    URL input = new URL(url + rune.getStyle().getName().toLowerCase() + "/" + internalName + "/" +
                            internalName + (rune == Rune.RUNE_8008 ? "temp" : "") + ".png");
                    HttpURLConnection conn = (HttpURLConnection) input.openConnection();
                    conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                    BufferedImage read = ImageIO.read(conn.getInputStream());
                    conn.getInputStream().close();
                    ImageIO.write(read, "png", new File("src/main/resources/runes/" + rune.getId() + ".png"));
                } catch (IOException e) {
                    log.error("Exception occurred while downloading rune image for rune " + rune.getName(), e);
                }
            }
        }
    }

    public static class ReforgedRuneDto {
        public String runePathName;
        public int runePathId;
        public String name;
        public int id;
        public String key;
        public String shortDesc;
        public String longDesc;
        public String icon;
        public volatile int slot = -1;
    }

    public static class ReforgedRuneSlotDto {
        public List<ReforgedRuneDto> runes;
    }

    public static class ReforgedRunePathDto {
        public String icon;
        public int id;
        public String key;
        public String name;
        List<ReforgedRuneSlotDto> slots;
    }

}
