package com.stirante.RuneChanger.model;

import com.stirante.RuneChanger.model.GameSettings.GameSettings;
import com.stirante.RuneChanger.model.GameSettings.GameSettingsUtil;
import com.stirante.RuneChanger.model.InputSettings.InputSettings;
import com.stirante.RuneChanger.model.InputSettings.InputSettingsUtil;

public class LeagueSettings {

    private InputSettings inputSettings;
    private GameSettings gameSettings;
    private String presetName;

    /**
     *
     * @param name identifier of the settings backup
     * @return boolean indicating if operation was successful
     */
    public boolean fetchSettings(String name) {
        gameSettings = GameSettingsUtil.getSettings();
        inputSettings = InputSettingsUtil.getSettings();
        presetName = name;
        if (gameSettings == null || inputSettings == null || presetName == null) {
            return false;
        }
        else {
            return true;
        }
    }
}
