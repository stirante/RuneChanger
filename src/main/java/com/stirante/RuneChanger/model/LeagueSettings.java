package com.stirante.RuneChanger.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.model.GameSettings.GameSettings;
import com.stirante.RuneChanger.model.InputSettings.InputSettings;
import com.stirante.lolclient.ClientApi;

import java.io.IOException;
import java.util.Map;

public class LeagueSettings {

    private static final String INPUT_SETTINGS_API_PATH = "/lol-game-settings/v1/input-settings";
    private static final String GAME_SETTINGS_API_PATH = "/lol-game-settings/v1/game-settings";
    private final Gson gson = new GsonBuilder().create();
    private final ClientApi api = RuneChanger.getInstance().getApi();
    private InputSettings inputSettings;
    private GameSettings gameSettings;
    private String presetName;

    private GameSettings getGameSettings() throws IOException {
        Map map = RuneChanger.getInstance().getApi().executeGet(GAME_SETTINGS_API_PATH, Map.class);
        GameSettings settings = gson.fromJson(gson.toJson(map), GameSettings.class);
        return settings;
    }

    private InputSettings getInputSettings() throws IOException {
        Map map = RuneChanger.getInstance().getApi().executeGet(INPUT_SETTINGS_API_PATH, Map.class);
        InputSettings settings = gson.fromJson(gson.toJson(map), InputSettings.class);
        return settings;
    }

    public String getIdentifier() {
        return this.presetName;
    }

    /**
     * @param name string identifier of the settings backup
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

        return true;
    }

    /**
     * @return boolean indicating if operation was successful
     */
    public boolean importSettings() {
        try {
            api.executePatch(INPUT_SETTINGS_API_PATH, inputSettings);
            api.executePatch(GAME_SETTINGS_API_PATH, gameSettings);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
