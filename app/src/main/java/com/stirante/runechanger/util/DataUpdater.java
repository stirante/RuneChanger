package com.stirante.runechanger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.stirante.justpipe.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String STYLE_ENUM_PREFIX = "package com.stirante.runechanger.api;\n" +
            "\n" +
            "public enum Style {\n";

    private static final String STYLE_ENUM_POSTFIX = "\n" +
            "    private final int id;\n" +
            "    private final String name;\n" +
            "\n" +
            "    Style(int id, String name) {\n" +
            "        this.id = id;\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Gets style by name\n" +
            "     *\n" +
            "     * @param name style name\n" +
            "     * @return style\n" +
            "     */\n" +
            "    public static Style getByName(String name) {\n" +
            "        for (Style style : values()) {\n" +
            "            if (style.name.equalsIgnoreCase(name)) {\n" +
            "                return style;\n" +
            "            }\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Gets style by id\n" +
            "     *\n" +
            "     * @param id style id\n" +
            "     * @return style\n" +
            "     */\n" +
            "    public static Style getById(int id) {\n" +
            "        for (Style style : values()) {\n" +
            "            if (style.id == id) {\n" +
            "                return style;\n" +
            "            }\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    public int getId() {\n" +
            "        return id;\n" +
            "    }\n" +
            "\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String toString() {\n" +
            "        return name() + \"(\" + name + \")\";\n" +
            "    }\n" +
            "}\n";

    private static final String RUNE_ENUM_PREFIX = "package com.stirante.runechanger.api;\n" +
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


    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().create();
        generateRunes(gson);
    }

    private static void generateRunes(Gson gson) throws IOException {
        InputStream in =
                getConnection("https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/perkstyles.json").getInputStream();
        StringBuilder sb = new StringBuilder();
        Styles styles = gson.fromJson(new InputStreamReader(in), Styles.class);
        in.close();
        in =
                getConnection("https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/perks.json").getInputStream();
        Rune[] runes = gson.fromJson(new InputStreamReader(in), Rune[].class);
        in.close();
        styles.styles.sort(Comparator.comparingInt(style -> style.id));
        List<Style> styleList = styles.styles;
        for (int i = 0; i < styleList.size(); i++) {
            Style style = styleList.get(i);
            sb.append("    STYLE_")
                    .append(style.id)
                    .append("(")
                    .append(style.id)
                    .append(", \"")
                    .append(style.name)
                    .append("\")");
            if (i == styles.styles.size() - 1) {
                sb.append(";\n");
            }
            else {
                sb.append(",\n");
            }
        }
        try {
            FileWriter writer = new FileWriter("api/src/main/java/com/stirante/runechanger/api/Style.java");
            writer.write(STYLE_ENUM_PREFIX + "    //Generated on " +
                    SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH)
                            .format(new Date()) + "\n" +
                    sb +
                    STYLE_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Exception occurred while generating Rune enum", e);
        }
        sb = new StringBuilder();
        Arrays.sort(runes, Comparator.comparingInt(o -> o.id));
        for (int i = 0; i < runes.length; i++) {
            Rune rune = runes[i];
            if (rune.id == 7000 || (rune.id > 5000 && rune.id < 6000)) {
                // Template rune or modifiers
                continue;
            }
            Style style = styles.styles.stream()
                    .filter(s -> s.slots.stream().anyMatch(slot -> slot.perks.stream().anyMatch(p -> Objects.equals(p, rune.id))))
                    .findFirst()
                    .orElse(null);
            if (style == null) {
                log.error("Rune {} has no style", rune.id);
                continue;
            }
            int slot = style.slots.indexOf(style.slots.stream().filter(s -> s.perks.stream().anyMatch(p -> Objects.equals(p, rune.id))).findFirst().orElseThrow());
            sb.append("    RUNE_")
                    .append(rune.id)
                    .append("(")
                    .append(rune.id)
                    .append(", ")
                    .append(style.id)
                    .append(", ")
                    .append(slot)
                    .append(", \"")
                    .append(rune.name)
                    .append("\")");
            if (i == runes.length - 1) {
                sb.append(";\n");
            }
            else {
                sb.append(",\n");
            }
        }
        try {
            FileWriter writer = new FileWriter("api/src/main/java/com/stirante/runechanger/api/Rune.java");
            writer.write(RUNE_ENUM_PREFIX + "    //Generated on " +
                    SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH)
                            .format(new Date()) + "\n" +
                    sb +
                    RUNE_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Exception occurred while generating Rune enum", e);
        }
        for (Style style : styles.styles) {
            downloadImage(style.id, style.iconPath);
        }
        for (Rune rune : runes) {
            downloadImage(rune.id, rune.iconPath);
        }
    }

    private static void downloadImage(int id, String iconPath) {
        String url = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default" + iconPath.replace("/lol-game-data/assets", "").toLowerCase();
        try {
            File file = new File("api/src/main/resources/runes/" + id + ".png");
            Pipe.from(getConnection(url).getInputStream()).to(file);
        } catch (IOException e) {
            log.error("Exception occurred while downloading rune icon", e);
        }
    }

    private static HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
        return conn;
    }


    public static class Styles {

        @SerializedName("schemaVersion")
        @Expose
        private Integer schemaVersion;
        @SerializedName("styles")
        @Expose
        private List<Style> styles = null;

    }

    public static class Slot {

        @SerializedName("type")
        @Expose
        private String type;
        @SerializedName("slotLabel")
        @Expose
        private String slotLabel;
        @SerializedName("perks")
        @Expose
        private List<Integer> perks = null;

    }

    public static class Style {

        @SerializedName("id")
        @Expose
        private Integer id;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("tooltip")
        @Expose
        private String tooltip;
        @SerializedName("iconPath")
        @Expose
        private String iconPath;
        @SerializedName("assetMap")
        @Expose
        private Map<String, String> assetMap;
        @SerializedName("isAdvanced")
        @Expose
        private Boolean isAdvanced;
        @SerializedName("allowedSubStyles")
        @Expose
        private List<Integer> allowedSubStyles = null;
        @SerializedName("subStyleBonus")
        @Expose
        private List<SubStyleBonu> subStyleBonus = null;
        @SerializedName("slots")
        @Expose
        private List<Slot> slots = null;
        @SerializedName("defaultPageName")
        @Expose
        private String defaultPageName;
        @SerializedName("defaultSubStyle")
        @Expose
        private Integer defaultSubStyle;
        @SerializedName("defaultPerks")
        @Expose
        private List<Integer> defaultPerks = null;
        @SerializedName("defaultPerksWhenSplashed")
        @Expose
        private List<Integer> defaultPerksWhenSplashed = null;
        @SerializedName("defaultStatModsPerSubStyle")
        @Expose
        private List<DefaultStatModsPerSubStyle> defaultStatModsPerSubStyle = null;

    }

    public static class DefaultStatModsPerSubStyle {

        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("perks")
        @Expose
        private List<Integer> perks = null;

    }


    public static class SubStyleBonu {

        @SerializedName("styleId")
        @Expose
        private Integer styleId;
        @SerializedName("perkId")
        @Expose
        private Integer perkId;

    }

    public static class Rune {

        @SerializedName("id")
        @Expose
        public Integer id;
        @SerializedName("name")
        @Expose
        public String name;
        @SerializedName("majorChangePatchVersion")
        @Expose
        public String majorChangePatchVersion;
        @SerializedName("tooltip")
        @Expose
        public String tooltip;
        @SerializedName("shortDesc")
        @Expose
        public String shortDesc;
        @SerializedName("longDesc")
        @Expose
        public String longDesc;
        @SerializedName("iconPath")
        @Expose
        public String iconPath;
        @SerializedName("endOfGameStatDescs")
        @Expose
        public List<String> endOfGameStatDescs = null;

    }

}
