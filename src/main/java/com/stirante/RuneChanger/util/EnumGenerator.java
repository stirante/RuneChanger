package com.stirante.RuneChanger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Internal class for generating Rune and Champion enums
 */
public class EnumGenerator {

    private static String API_KEY = "";
    private static final String URL_BASE = "https://eun1.api.riotgames.com";

    private static final String RUNE_ENUM_PREFIX = "package com.stirante.RuneChanger.model;\n" +
            "\n" +
            "public enum Rune {\n";
    private static final String RUNE_ENUM_POSTFIX = "\n" +
            "\n" +
            "    private final int id;\n" +
            "    private final Style style;\n" +
            "    private final int slot;\n" +
            "    private final String name;\n" +
            "\n" +
            "    Rune(int id, int styleId, int slot, String name) {\n" +
            "        this.id = id;\n" +
            "        style = Style.getById(styleId);\n" +
            "        this.slot = slot;\n" +
            "        this.name = name;\n" +
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
            "     * Get rune by name\n" +
            "     *\n" +
            "     * @param name rune name\n" +
            "     * @return rune\n" +
            "     */\n" +
            "    public static Rune getByName(String name) {\n" +
            "        for (Rune rune : values()) {\n" +
            "            if (rune.name.equalsIgnoreCase(name) || rune.name.replaceAll(\"'\", \"’\").equalsIgnoreCase(name)) return rune;\n" +
            "        }\n" +
            "        System.out.println(name + \" not found\");\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String toString() {\n" +
            "        return name() + \"(\" + name + \")\";\n" +
            "    }\n" +
            "}\n";

    private static final String CHAMPION_ENUM_PREFIX = "package com.stirante.RuneChanger.model;\n" +
            "\n" +
            "public enum Champion {\n";
    private static final String CHAMPION_ENUM_POSTFIX = "\n" +
            "\n" +
            "    private final int id;\n" +
            "    private final String internalName;\n" +
            "    private final String name;\n" +
            "    private final String alias;\n" +
            "\n" +
            "    Champion(int id, String internalName, String name, String alias) {\n" +
            "        this.id = id;\n" +
            "        this.internalName = internalName;\n" +
            "        this.name = name;\n" +
            "        this.alias = alias;\n" +
            "    }\n" +
            "\n" +
            "    Champion(int id, String internalName, String name) {\n" +
            "        this(id, internalName, name, name);\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion id\n" +
            "     *\n" +
            "     * @return champion id\n" +
            "     */\n" +
            "    public int getId() {\n" +
            "        return id;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get riot internal champion name\n" +
            "     *\n" +
            "     * @return internal champion name\n" +
            "     */\n" +
            "    public String getInternalName() {\n" +
            "        return internalName;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion name\n" +
            "     *\n" +
            "     * @return champion name\n" +
            "     */\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get alternative champion name\n" +
            "     *\n" +
            "     * @return alias\n" +
            "     */\n" +
            "    public String getAlias() {\n" +
            "        return alias;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion by id\n" +
            "     *\n" +
            "     * @param id id\n" +
            "     * @return champion\n" +
            "     */\n" +
            "    public static Champion getById(int id) {\n" +
            "        for (Champion champion : values()) {\n" +
            "            if (champion.id == id) return champion;\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "}\n";

    private static InputStream getEndpoint(String endpoint) throws IOException {
        URL url = new URL(URL_BASE + endpoint);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (urlConnection.getResponseCode() == 403) throw new RuntimeException("Regenerate API key");
        return urlConnection.getInputStream();
    }

    public static void main(String[] args) throws IOException {
        if (API_KEY.isEmpty()) throw new IllegalStateException("API_KEY is empty!");
        Gson gson = new GsonBuilder().create();
        generateChampions(gson);
        generateRunes(gson);
    }

    private static void generateChampions(Gson gson) throws IOException {
        InputStream in = getEndpoint("/lol/static-data/v3/champions?locale=en_US&dataById=false&api_key=" + API_KEY);
        StringBuilder sb = new StringBuilder();
        ChampionListDto champions = gson.fromJson(new InputStreamReader(in), ChampionListDto.class);
        in.close();
        List<ChampionDto> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));
        for (int i = 0; i < values.size(); i++) {
            ChampionDto champion = values.get(i);
            sb.append("    ").append(champion.key.toUpperCase()).append("(").append(champion.id).append(", \"").append(champion.key).append("\", \"").append(champion.name).append("\", \"").append(champion.name.replaceAll(" ", "")).append("\")");
            if (i == values.size() - 1) sb.append(";\n");
            else sb.append(",\n");
        }
        try {
            FileWriter writer = new FileWriter(new File("src/main/java/com/stirante/RuneChanger/model/Champion.java"));
            writer.write(CHAMPION_ENUM_PREFIX + "    //Generated on " + SimpleDateFormat.getDateTimeInstance().format(new Date()) + "\n" + sb.toString() + CHAMPION_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateRunes(Gson gson) throws IOException {
        InputStream in = getEndpoint("/lol/static-data/v3/reforged-rune-paths?api_key=" + API_KEY);
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
            sb.append("    RUNE_").append(rune.id).append("(").append(rune.id).append(", ").append(rune.runePathId).append(", ").append(rune.slot).append(", \"").append(rune.name).append("\")");
            if (i == runes1.size() - 1) sb.append(";\n");
            else sb.append(",\n");
        }
        try {
            FileWriter writer = new FileWriter(new File("src/main/java/com/stirante/RuneChanger/model/Rune.java"));
            writer.write(RUNE_ENUM_PREFIX + "    //Generated on " + SimpleDateFormat.getDateTimeInstance().format(new Date()) + "\n" + sb.toString() + RUNE_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        List<ReforgedRuneSlotDto> slots;
        public String icon;
        public int id;
        public String key;
        public String name;
    }

    public static class ChampionListDto {
        public String version;
        public String type;
        public String format;
        public Map<String, String> keys;
        public Map<String, ChampionDto> data;
    }

    public static class ChampionDto {
        public String title;
        public int id;
        public String key;
        public String name;
    }

}
