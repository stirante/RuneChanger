package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.util.AutoStartUtils;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingsController {
    private final Stage stage;
    public CheckBox antiAway;
    public CheckBox autoAccept;
    public CheckBox quickReplies;
    public CheckBox autoUpdate;
    public CheckBox alwaysOnTop;
    public CheckBox autoStart;
    public CheckBox forceEnglish;
    public CheckBox experimental;
    public CheckBox autoSync;
    public CheckBox smartDisenchant;
    public Pane container;

    public SettingsController(Stage stage) {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Settings.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadPreferences();
    }

    @FXML
    void handleCheckboxPressed(ActionEvent e) {
        System.out.println(e);
        CheckBox target = (CheckBox) e.getTarget();
        if (target == autoStart) {
            AutoStartUtils.setAutoStart(target.isSelected());
        }
        if (target == autoAccept) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.AUTO_ACCEPT, String.valueOf(target.isSelected()));
        }
        else if (target == antiAway) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.ANTI_AWAY, String.valueOf(target.isSelected()));
        }
        else if (target == quickReplies) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.QUICK_REPLIES, String.valueOf(target.isSelected()));
        }
        else if (target == autoUpdate) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.AUTO_UPDATE, String.valueOf(target.isSelected()));
        }
        else if (target == smartDisenchant) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.SMART_DISENCHANT, String.valueOf(target.isSelected()));
        }
        else if (target == experimental) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, String.valueOf(target
                    .isSelected()));
        }
        else if (target == autoSync) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.AUTO_SYNC, String.valueOf(target
                    .isSelected()));
            tryRestart();
        }
        else if (target == alwaysOnTop) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, String.valueOf(target.isSelected()));
            stage.setAlwaysOnTop(target.isSelected());
        }
        else if (target == forceEnglish) {
            SimplePreferences.putValue(SimplePreferences.SettingsKeys.FORCE_ENGLISH, String.valueOf(target.isSelected()));
            tryRestart();
        }
        SimplePreferences.save();
    }

    private void tryRestart() {
        boolean restart = Settings.openYesNoDialog(LangHelper.getLang()
                .getString("restart_necessary"), LangHelper.getLang()
                .getString("restart_necessary_description"));
        if (restart) {
            restartProgram();
        }
    }

    private void loadPreferences() {
        setupPreference(SimplePreferences.SettingsKeys.QUICK_REPLIES, "false", quickReplies);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_ACCEPT, "false", autoAccept);
        setupPreference(SimplePreferences.SettingsKeys.ANTI_AWAY, "false", antiAway);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_UPDATE, "true", autoUpdate);
        setupPreference(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, "false", experimental);
        setupPreference(SimplePreferences.SettingsKeys.FORCE_ENGLISH, "false", forceEnglish);
        setupPreference(SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, "false", alwaysOnTop);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_SYNC, "false", autoSync);
        setupPreference(SimplePreferences.SettingsKeys.SMART_DISENCHANT, "false", smartDisenchant);

        if (AutoStartUtils.isAutoStartEnabled()) {
            autoStart.setSelected(true);
        }
    }

    private void setupPreference(String key, String defaultValue, CheckBox checkbox) {
        if (!SimplePreferences.containsKey(key)) {
            SimplePreferences.putValue(key, defaultValue);
        }
        if (SimplePreferences.getValue(key, defaultValue).equals("true")) {
            Platform.runLater(() -> checkbox.setSelected(true));
        }
    }

    private void restartProgram() {
        SimplePreferences.save();
        try {
            Runtime.getRuntime().exec("wscript silent.vbs open.bat");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

}
