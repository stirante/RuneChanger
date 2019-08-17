package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.RuneChanger;

import java.io.IOException;
import java.util.Map;

public class GameSettingsUtil {

    private static final String API_PATH = "/lol-game-settings/v1/game-settings";

    public static GameSettings getSettings() {
        Map map = null;
        try {
            map = RuneChanger.getInstance().getApi().executeGet(API_PATH, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Gson gson = new GsonBuilder().create();
        GameSettings settings = gson.fromJson(gson.toJson(map), GameSettings.class);
        return settings;
    }

}
