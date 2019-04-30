package com.stirante.RuneChanger.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
                System.out.println("Non standard character: \"" + str.charAt(i) + "\"");
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
        String replace = str.replace("[","");
        String replace1 = replace.replace("]","");
        replace1 = replace1.replace(" ", "");
        List<String> myList = new ArrayList<>(Arrays.asList(replace1.split(",")));
        return myList;
    }
}
