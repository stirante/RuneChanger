package com.stirante.RuneChanger.model.InputSettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.RuneChanger;

import java.io.IOException;
import java.util.Map;

public class InputSettingsUtil {

    private static final String API_PATH = "/lol-game-settings/v1/input-settings";

    public static InputSettings getSettings() {
        Map map = null;
        try {
            map = RuneChanger.getInstance().getApi().executeGet(API_PATH, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Gson gson = new GsonBuilder().create();
        InputSettings settings = gson.fromJson(gson.toJson(map), InputSettings.class);
        return settings;
    }
}
