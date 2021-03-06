package com.stirante.runechanger.model.client;

import com.google.gson.Gson;
import com.stirante.runechanger.sourcestore.impl.UGGSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Patch {
    private static final Logger log = LoggerFactory.getLogger(UGGSource.class);
    private static List<Patch> patchCache = null;
    private final int[] patchNumbers = new int[3];

    private Patch(int[] patchNumbers) {
        this.patchNumbers[0] = patchNumbers[0];
        this.patchNumbers[1] = patchNumbers[1];
        this.patchNumbers[2] = patchNumbers[2];
    }

    private static void initPatchCache() {
        if (patchCache != null) {
            return;
        }
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL("https://ddragon.leagueoflegends.com/api/versions.json").openConnection();
            conn.connect();
            String[] jsonData = new Gson().fromJson(new InputStreamReader(conn.getInputStream()), String[].class);
            conn.getInputStream().close();
            patchCache = new ArrayList<>();
            for (String patchString : jsonData) {
                Patch p = Patch.fromString(patchString);
                if (p != null) {
                    patchCache.add(p);
                }
            }
        } catch (IOException e) {
            log.error("Failed to get the patch data.");
        }
    }

    public static Patch getLatest() {
        initPatchCache();
        return patchCache.get(0);
    }

    public static List<Patch> getLatest(int n) {
        initPatchCache();
        List<Patch> patches = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i + 1 > patchCache.size()) {
                break;
            }
            patches.add(patchCache.get(i));
        }
        return patches;
    }

    public static Patch fromString(String patchString) {
        String[] patchStringSplit = patchString.split(Pattern.quote("."), 3);
        if (patchStringSplit.length != 3) {
            return null;
        }
        try {
            int[] patchNumbers = new int[3];
            for (int i = 0; i < 3; i++) {
                patchNumbers[i] = Integer.parseInt(patchStringSplit[i]);
            }
            return new Patch(patchNumbers);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String toString() {
        return toShortString();
    }

    public String format(String format) {
        return String.format(format, patchNumbers[0], patchNumbers[1] ,patchNumbers[2]);
    }

    public int[] getPatchNumbers() {
        return patchNumbers.clone();
    }

    public String toFullString() {
        return format("%d.%d.%d");
    }

    public String toShortString() {
        return format("%d.%d");
    }
}

