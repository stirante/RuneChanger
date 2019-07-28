package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.util.AutoStartUtils;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.PathUtils;
import com.stirante.RuneChanger.util.SimplePreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static com.stirante.RuneChanger.gui.Settings.mainStage;

@Slf4j
public class SettingsController implements Initializable {

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
        Platform.runLater(() -> loadPreferences());
    }

    @FXML
    void handleCheckboxPressed(ActionEvent e) {
        JFXCheckBox target = (JFXCheckBox) e.getTarget();
        if (target == autoStartCB) {
            AutoStartUtils.setAutoStart(target.isSelected());
        }
        if (target == autoQueueCB) {
            SimplePreferences.putValue("autoAccept", String.valueOf(target.isSelected()));
        }
        else if (target == noAwayCB) {
            SimplePreferences.putValue("antiAway", String.valueOf(target.isSelected()));
        }
        else if (target == quickRepliesCB) {
            SimplePreferences.putValue("quickReplies", String.valueOf(target.isSelected()));
        }
        else if (target == autoUpdateCB) {
            SimplePreferences.putValue("autoUpdate", String.valueOf(target.isSelected()));
        }
        else if (target == alwaysOnTopCB) {
            SimplePreferences.putValue("alwaysOnTop", String.valueOf(target.isSelected()));
            mainStage.setAlwaysOnTop(target.isSelected());
        }
        else if (target == forceEnglishCB) {
            SimplePreferences.putValue("force_english", String.valueOf(target.isSelected()));
            boolean restart =
                    ControllerUtil.getInstance().showConfirmationScreen(LangHelper.getLang().getString("restart_necessary"), LangHelper.getLang()
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
        if (SimplePreferences.getValue(key) == null) {
            SimplePreferences.putValue(key, defaultValue);
        }
        if (SimplePreferences.getValue(key).equals("true")) {
            Platform.runLater(() -> checkbox.setSelected(true));
        }
    }

    private void restartProgram() {
        SimplePreferences.save();
        try {
            //From https://stackoverflow.com/a/4194224/6459649
            //find path to the current jar
            final File currentJar =
                    new File(SettingsController.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());

            //if it's not a jar, just close it (probably running from IDE)
            if (!currentJar.getName().endsWith(".jar")) {
                System.exit(0);
            }

            //construct command and run it
            final ArrayList<String> command = new ArrayList<>();
            command.add(PathUtils.getJavawPath());
            command.add("-jar");
            command.add(currentJar.getPath());

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

}
