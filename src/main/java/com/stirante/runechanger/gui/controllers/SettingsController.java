package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.ControllerUtil;
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
            SimplePreferences.addSettingsElement("autoAccept", String.valueOf(target.isSelected()));
        }
        else if (target == antiAway) {
            SimplePreferences.addSettingsElement("antiAway", String.valueOf(target.isSelected()));
        }
        else if (target == quickReplies) {
            SimplePreferences.addSettingsElement("quickReplies", String.valueOf(target.isSelected()));
        }
        else if (target == autoUpdate) {
            SimplePreferences.addSettingsElement("autoUpdate", String.valueOf(target.isSelected()));
        }
        else if (target == experimental) {
            SimplePreferences.addSettingsElement("devChannel", String.valueOf(target.isSelected()));
        }
        else if (target == alwaysOnTop) {
            SimplePreferences.addSettingsElement("alwaysOnTop", String.valueOf(target.isSelected()));
            stage.setAlwaysOnTop(target.isSelected());
        }
        else if (target == forceEnglish) {
            SimplePreferences.addSettingsElement("forceEnglish", String.valueOf(target.isSelected()));
            boolean restart = Settings.openYesNoDialog(LangHelper.getLang()
                    .getString("restart_necessary"), LangHelper.getLang()
                    .getString("restart_necessary_description"));
//                    ControllerUtil
//                            .showConfirmationScreen(LangHelper.getLang()
//                                    .getString("restart_necessary"), LangHelper.getLang()
//                                    .getString("restart_necessary_description"));
            if (restart) {
                restartProgram();
            }
        }
        SimplePreferences.save();
    }

    private void loadPreferences() {
        setupPreference("quickReplies", "false", quickReplies);
        setupPreference("autoAccept", "false", autoAccept);
        setupPreference("antiAway", "false", antiAway);
        setupPreference("autoUpdate", "true", autoUpdate);
        setupPreference("devChannel", "false", experimental);
        setupPreference("forceEnglish", "false", forceEnglish);
        setupPreference("alwaysOnTop", "false", alwaysOnTop);

        if (AutoStartUtils.isAutoStartEnabled()) {
            autoStart.setSelected(true);
        }
    }

    private void setupPreference(String key, String defaultValue, CheckBox checkbox) {
        if (SimplePreferences.getSettingsValue(key) == null) {
            SimplePreferences.addSettingsElement(key, defaultValue);
        }
        if (SimplePreferences.getSettingsValue(key).equals("true")) {
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
