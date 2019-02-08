package com.stirante.RuneChanger.gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXToggleButton;
import com.stirante.RuneChanger.util.RuneBook;
import com.stirante.RuneChanger.util.SimplePreferences;
import javafx.animation.RotateTransition;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import static com.stirante.RuneChanger.gui.Settings.*;

public class SettingsController {

    @FXML
    private JFXButton disenchantBtn;
    @FXML
    private JFXButton craftKeyBtn;
    @FXML
    private JFXButton addBtn;
    @FXML
    private JFXButton removeBtn;
    @FXML
    private ImageView btn_settings;
    @FXML
    private ImageView btn_exit;
    @FXML
    private ImageView btn_credits;
    @FXML
    private ImageView btn_runebook;
    @FXML
    private ImageView syncButton;
    @FXML
    private AnchorPane settingsPane;
    @FXML
    private AnchorPane creditsPane;
    @FXML
    private AnchorPane runebookPane;
    @FXML
    private JFXToggleButton quickReplyBtn;
    @FXML
    private JFXToggleButton autoQueueBtn;
    @FXML
    private JFXToggleButton noAwayBtn;
    @FXML
    private JFXListView<Label> localRunes, clientRunes;

    private static void rotateSyncButton(ImageView syncButton) {
        if (syncButton.isDisabled()) {
            return;
        }
        syncButton.setDisable(true);
        RotateTransition rt = new RotateTransition(Duration.millis(2000), syncButton);
        rt.setByAngle(360);
        rt.setCycleCount(1);
        rt.play();
        rt.setOnFinished(event -> syncButton.setDisable(false));
    }

    static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleMenuSelection(MouseEvent event) {
        if (event.getTarget() == btn_settings) {
            settingsPane.setVisible(true);
            runebookPane.setVisible(false);
            creditsPane.setVisible(false);

        }
        else if (event.getTarget() == btn_credits) {
            settingsPane.setVisible(false);
            runebookPane.setVisible(false);
            creditsPane.setVisible(true);
        }
        else if (event.getTarget() == btn_runebook) {
            runebookPane.setVisible(true);
            creditsPane.setVisible(false);
            settingsPane.setVisible(false);
        }
        else if (event.getTarget() == btn_exit) {
            mainStage.hide();
        }
    }

    @FXML
    void handleSettingsButtonPressed(Event e) {
        if (e.getTarget() == craftKeyBtn) {
            craftKeys();
        }
        else if (e.getTarget() == disenchantBtn) {
            disenchantChampions();
        }
    }

    @FXML
    void handleToggleButtonPressed(Event e) {
        if (e.getTarget() == autoQueueBtn) {
            SimplePreferences.putValue("autoAccept", String.valueOf(autoQueueBtn.isSelected()));
        }
        else if (e.getTarget() == noAwayBtn) {
            SimplePreferences.putValue("antiAway", String.valueOf(noAwayBtn.isSelected()));
        }
        else if (e.getTarget() == quickReplyBtn) {
            SimplePreferences.putValue("quickReplies", String.valueOf(quickReplyBtn.isSelected()));
        }
        SimplePreferences.save();
    }

    @FXML
    void handleSyncBtn() {
        rotateSyncButton(syncButton);
        RuneBook.refreshClientRunes(clientRunes);
    }

    @FXML
    void handleRunebookButtonPressed(Event e) {
        if (e.getTarget() == addBtn) {
            RuneBook.importLocalRunes(localRunes, clientRunes);
        }
        else if (e.getTarget() == removeBtn) {
            RuneBook.deleteRunePage(localRunes);
        }
    }

    @FXML
    void initialize() {
        SimplePreferences.load();
        loadPreferences();
        settingsPane.setVisible(true);
    }

    private void loadPreferences() {
        if (SimplePreferences.getValue("autoAccept") != null &&
                SimplePreferences.getValue("autoAccept").equals("true")) {
            autoQueueBtn.setSelected(true);
        }
        if (SimplePreferences.getValue("antiAway") != null && SimplePreferences.getValue("antiAway").equals("true")) {
            noAwayBtn.setSelected(true);
        }
        if (SimplePreferences.getValue("quickReplies") != null &&
                SimplePreferences.getValue("quickReplies").equals("true")) {
            quickReplyBtn.setSelected(true);
        }
        if (SimplePreferences.runeBookValues != null && !SimplePreferences.runeBookValues.isEmpty()) {
            RuneBook.refreshLocalRunes(localRunes);
        }
    }

}
