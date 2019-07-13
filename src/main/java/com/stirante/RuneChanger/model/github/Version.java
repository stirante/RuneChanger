package com.stirante.RuneChanger.model.github;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class Version {

    public static final Version INSTANCE;

    static {
        Version inst = new Version();
        try {
            InputStream stream = Version.class.getResourceAsStream("/version.json");
            inst = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create().fromJson(new InputStreamReader(stream), Version.class);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        INSTANCE = inst;
    }

    public String branch;
    public Date buildTime;
    public String version;
    public String commitId;
    public String commitIdAbbrev;

}