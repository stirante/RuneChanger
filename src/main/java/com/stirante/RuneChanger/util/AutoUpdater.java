package com.stirante.RuneChanger.util;

import com.google.gson.Gson;
import com.stirante.RuneChanger.gui.Constants;
import com.stirante.RuneChanger.model.github.Release;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AutoUpdater {

    public static boolean check() throws IOException {
        URL url = new URL(Constants.LATEST_RELEASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream in = conn.getInputStream();
        Release latest = new Gson().fromJson(new InputStreamReader(in), Release.class);
        in.close();
        System.out.println(latest.tagName);
        return latest.tagName.equalsIgnoreCase(Constants.VERSION_STRING);
    }

    public static void main(String[] args) throws IOException {
        System.out.println(check());
    }

}
