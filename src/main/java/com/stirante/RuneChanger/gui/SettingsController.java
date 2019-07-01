package com.stirante.RuneChanger.gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXToggleButton;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.RuneBook;
import com.stirante.RuneChanger.util.SimplePreferences;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

import static com.stirante.RuneChanger.gui.Settings.mainStage;

public class SettingsController {

    @FXML
    private JFXButton disenchantBtn;
    @FXML
    private JFXButton craftKeyBtn;
    @FXML
    private JFXButton addBtn;
    @FXML
    private JFXButton loadBtn;
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
    private AnchorPane mainPane;
    @FXML
    private AnchorPane toolbarPane;
    @FXML
    private JFXToggleButton quickReplyBtn;
    @FXML
    private JFXToggleButton force_english_btn;
    @FXML
    private JFXToggleButton autoQueueBtn;
    @FXML
    private JFXToggleButton noAwayBtn;
    @FXML
    private JFXToggleButton autoUpdateBtn;
    @FXML
    private JFXListView<Label> localRunes, clientRunes;

    private static AnchorPane currentPane = null;
    private Settings settings;

    private void loadPreferences() {
        setupPreference("quickReplies", "false", quickReplyBtn);
        setupPreference("autoAccept", "false", autoQueueBtn);
        setupPreference("antiAway", "false", noAwayBtn);
        setupPreference("autoUpdate", "true", autoUpdateBtn);
        setupPreference("force_english", "false", force_english_btn);

        if (!SimplePreferences.runeBookValues.isEmpty()) {
            RuneBook.refreshLocalRunes(localRunes);
        }
    }

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

    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private FadeTransition fade(AnchorPane pane, int duration, int from, int to) {
        FadeTransition ft = new FadeTransition(Duration.millis(duration), pane);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    @FXML
    void handleMenuSelection(MouseEvent event) {
        if (event.getTarget() == btn_settings && currentPane != settingsPane) {
            settingsPane.setVisible(true);
            runebookPane.setVisible(false);
            creditsPane.setVisible(false);
            fade(currentPane, 700, 1, 0).playFromStart();
            fade(settingsPane, 700, 0, 1).playFromStart();
            currentPane = settingsPane;
        }
        else if (event.getTarget() == btn_credits && currentPane != creditsPane) {
            settingsPane.setVisible(false);
            runebookPane.setVisible(false);
            creditsPane.setVisible(true);
            fade(currentPane, 700, 1, 0).playFromStart();
            fade(creditsPane, 700, 0, 1).playFromStart();
            currentPane = creditsPane;
        }
        else if (event.getTarget() == btn_runebook && currentPane != runebookPane) {
            runebookPane.setVisible(true);
            creditsPane.setVisible(false);
            settingsPane.setVisible(false);
            fade(currentPane, 700, 1, 0).playFromStart();
            fade(runebookPane, 700, 0, 1).playFromStart();
            currentPane = runebookPane;
            Platform.runLater(() -> RuneBook.refreshClientRunes(clientRunes));
        }
        else if (event.getTarget() == btn_exit) {
            mainStage.hide();
        }
    }

    @FXML
    void handleSettingsButtonPressed(Event e) {
        if (e.getTarget() == craftKeyBtn) {
            settings.craftKeys();
        }
        else if (e.getTarget() == disenchantBtn) {
            settings.disenchantChampions();
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
        else if (e.getTarget() == autoUpdateBtn) {
            SimplePreferences.putValue("autoUpdate", String.valueOf(autoUpdateBtn.isSelected()));
        }
        else if (e.getTarget() == force_english_btn) {
            SimplePreferences.putValue("force_english", String.valueOf(force_english_btn.isSelected()));
            boolean restart =
                    showConfirmationScreen(LangHelper.getLang().getString("restart_necessary"), LangHelper.getLang()
                            .getString("restart_necessary_description"));
            if (restart) {
                SimplePreferences.save();
                try {
                    //From https://stackoverflow.com/a/4194224/6459649
                    //find java executable path
                    final String javaBin =
                            System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

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
                    command.add(javaBin);
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
        else if (e.getTarget() == loadBtn) {
            RuneBook.loadAction(localRunes, clientRunes);
        }
    }

    @FXML
    void onListViewKeyPressed(KeyEvent event) {
        if (event.getCode().equals(KeyCode.C) && event.isControlDown()) {
            RuneBook.handleCtrlC((JFXListView<Label>) event.getSource());
        }
        else if (event.getCode().equals(KeyCode.V) && event.isControlDown()) {
            RuneBook.handleCtrlV((JFXListView<Label>) event.getSource());
        }
    }

    @FXML
    void initialize() {
        SimplePreferences.load();
        loadPreferences();
        settingsPane.setVisible(true);
        currentPane = settingsPane;
        fade(mainPane, 1750, 0, 1).playFromStart();
    }

    private void setupPreference(String key, String defaultValue, JFXToggleButton button) {
        if (SimplePreferences.getValue(key) == null) {
            SimplePreferences.putValue(key, defaultValue);
        }
        if (SimplePreferences.getValue(key).equals("true")) {
            Platform.runLater(() -> button.setSelected(true));
        }
    }

    private boolean showConfirmationScreen(String title, String message) {
        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        int dialogResult = JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.YES_NO_OPTION);
        frame.dispose();
        if (dialogResult == JOptionPane.YES_OPTION) {
            return true;
        }
        else {
            return false;
        }
    }

    public void init(Settings settings) {
        this.settings = settings;
    }
}
