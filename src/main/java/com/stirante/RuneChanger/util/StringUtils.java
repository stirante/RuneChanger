package com.stirante.RuneChanger.util;

import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class StringUtils {

    /**
     * Replacements for non standard characters
     */
    private static final HashMap<String, String> REPLACEMENT = new HashMap<>();

    static {
        REPLACEMENT.put("’", "'");
        REPLACEMENT.put("‘", "'");
        REPLACEMENT.put("…", "...");
        REPLACEMENT.put("”", "\"");
    }

    /**
     * Replace non standard characters so they won't break client
     *
     * @param str string
     * @return fixed string
     */
    public static String fixString(String str) {
        for (String s : REPLACEMENT.keySet()) {
            if (str.contains(s)) {
                str = str.replaceAll(s, REPLACEMENT.get(s));
            }
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) > 127) {
                log.warn("Non standard character: \"" + str.charAt(i) + "\"");
            }
        }
        if (!Normalizer.isNormalized(str, Normalizer.Form.NFC)) {
            str = Normalizer.normalize(str, Normalizer.Form.NFC);
        }
        return str;
    }

    /**
     * Connvert a string structured like a array to a list
     *
     * @param str string
     * @return List<String>
     */
    public static List<String> stringToList(String str) {
        log.debug("String before list conversion: " + str);
        String replace = str.replace("[", "");
        String replace1 = replace.replace("]", "");
        String[] parts = replace1.split(",", 2);
        String part1 = parts[0];
        replace1 = parts[1].replace(" ", "");
        replace1 = part1 + "," + replace1;
        List<String> myList = new ArrayList<>(Arrays.asList(replace1.split(",")));
        log.debug("List after string to list conversion: " + myList);
        return myList;
    }
}
