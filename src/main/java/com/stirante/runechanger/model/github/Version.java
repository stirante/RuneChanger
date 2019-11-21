package com.stirante.runechanger.model.github;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Version {

    public static final Version INSTANCE;

    static {
        Version inst = new Version();
        try {
            InputStream stream = Version.class.getResourceAsStream("/version.json");
            //Add our own date deserializer, which will return current date in case of parsing error
            //It's added, so it won't crash when running from IDE
            JsonDeserializer<Date> dateDeserializer = new JsonDeserializer<>() {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

                @Override
                public Date deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                        throws JsonParseException {
                    try {
                        return df.parse(json.getAsString());
                    } catch (ParseException e) {
                        return new Date();
                    }
                }
            };
            inst = new GsonBuilder().registerTypeAdapter(Date.class, dateDeserializer).create().fromJson(new InputStreamReader(stream), Version.class);
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