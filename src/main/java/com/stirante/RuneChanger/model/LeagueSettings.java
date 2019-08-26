package com.stirante.RuneChanger.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.model.GameSettings.GameSettings;
import com.stirante.RuneChanger.model.InputSettings.InputSettings;

import java.io.IOException;
import java.util.Map;

public class LeagueSettings {

    private static final String INPUT_SETTINGS_API_PATH = "/lol-game-settings/v1/input-settings";
    private static final String GAME_SETTINGS_API_PATH = "/lol-game-settings/v1/game-settings";

    private InputSettings inputSettings;
    private GameSettings gameSettings;
    private String presetName;
    Gson gson = new GsonBuilder().create();

    private GameSettings getGameSettings() throws IOException{
        Map map = RuneChanger.getInstance().getApi().executeGet(GAME_SETTINGS_API_PATH, Map.class);
        GameSettings settings = gson.fromJson(gson.toJson(map), GameSettings.class);
        return settings;
    }

    private InputSettings getInputSettings() throws IOException {
        Map map = RuneChanger.getInstance().getApi().executeGet(INPUT_SETTINGS_API_PATH, Map.class);
        InputSettings settings = gson.fromJson(gson.toJson(map), InputSettings.class);
        return settings;
    }
    /**
     *
     * @param name identifier of the settings backup
     * @return boolean indicating if operation was successful
     */
    public boolean fetchSettings(String name) {
        try {
            gameSettings = getGameSettings();
            inputSettings = getInputSettings();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        presetName = name;
        if (gameSettings == null || inputSettings == null || presetName == null) {
            return false;
        }
        else {
            return true;
        }
    }
}
