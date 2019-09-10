package com.stirante.runechanger.gui.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.stirante.runechanger.gui.ControllerUtil;
import com.stirante.runechanger.util.AutoStartUtils;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private JFXCheckBox noAwayCB;

    @FXML
    private JFXCheckBox autoQueueCB;

    @FXML
    private JFXCheckBox quickRepliesCB;

    @FXML
    private JFXCheckBox autoUpdateCB;

    @FXML
    private JFXCheckBox alwaysOnTopCB;

    @FXML
    private JFXCheckBox autoStartCB;

    @FXML
    private JFXCheckBox forceEnglishCB;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Settings Controller initializing");
        Platform.runLater(this::loadPreferences);
    }

    @FXML
    void handleCheckboxPressed(ActionEvent e) {
        JFXCheckBox target = (JFXCheckBox) e.getTarget();
        if (target == autoStartCB) {
            AutoStartUtils.setAutoStart(target.isSelected());
        }
        if (target == autoQueueCB) {
            SimplePreferences.addSettingsElement("autoAccept", String.valueOf(target.isSelected()));
        }
        else if (target == noAwayCB) {
            SimplePreferences.addSettingsElement("antiAway", String.valueOf(target.isSelected()));
        }
        else if (target == quickRepliesCB) {
            SimplePreferences.addSettingsElement("quickReplies", String.valueOf(target.isSelected()));
        }
        else if (target == autoUpdateCB) {
            SimplePreferences.addSettingsElement("autoUpdate", String.valueOf(target.isSelected()));
        }
        else if (target == alwaysOnTopCB) {
            SimplePreferences.addSettingsElement("alwaysOnTop", String.valueOf(target.isSelected()));
//            mainStage.setAlwaysOnTop(target.isSelected());
        }
        else if (target == forceEnglishCB) {
            SimplePreferences.addSettingsElement("force_english", String.valueOf(target.isSelected()));
            boolean restart =
                    ControllerUtil.getInstance()
                            .showConfirmationScreen(LangHelper.getLang()
                                    .getString("restart_necessary"), LangHelper.getLang()
                                    .getString("restart_necessary_description"));
            if (restart) {
                restartProgram();
            }
        }
        SimplePreferences.save();
    }

    private void loadPreferences() {
        setupPreference("quickReplies", "false", quickRepliesCB);
        setupPreference("autoAccept", "false", autoQueueCB);
        setupPreference("antiAway", "false", noAwayCB);
        setupPreference("autoUpdate", "true", autoUpdateCB);
        setupPreference("force_english", "false", forceEnglishCB);
        setupPreference("alwaysOnTop", "false", alwaysOnTopCB);

        if (AutoStartUtils.isAutoStartEnabled()) {
            autoStartCB.setSelected(true);
        }
    }

    private void setupPreference(String key, String defaultValue, JFXCheckBox checkbox) {
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
