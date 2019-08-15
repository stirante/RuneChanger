package com.stirante.RuneChanger.gui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXToggleButton;
import com.stirante.RuneChanger.model.LogRequest;
import com.stirante.RuneChanger.util.*;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import static com.stirante.RuneChanger.gui.Settings.getMainStage;

@Slf4j
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
    private JFXToggleButton autostart_btn;
    @FXML
    private JFXToggleButton noAwayBtn;
    @FXML
    private JFXToggleButton alwaysOnTopBtn;
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
        setupPreference("alwaysOnTop", "false", alwaysOnTopBtn);

        if (AutoStartUtils.isAutoStartEnabled()) {
            autostart_btn.setSelected(true);
        }

        if (!SimplePreferences.getRuneBookValues().isEmpty()) {
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
            getMainStage().hide();
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
        if (e.getTarget() == autostart_btn) {
            AutoStartUtils.setAutoStart(autostart_btn.isSelected());
        }
        if (e.getTarget() == autoQueueBtn) {
            SimplePreferences.addSettingsElement("autoAccept", String.valueOf(autoQueueBtn.isSelected()));
        }
        else if (e.getTarget() == noAwayBtn) {
            SimplePreferences.addSettingsElement("antiAway", String.valueOf(noAwayBtn.isSelected()));
        }
        else if (e.getTarget() == quickReplyBtn) {
            SimplePreferences.addSettingsElement("quickReplies", String.valueOf(quickReplyBtn.isSelected()));
        }
        else if (e.getTarget() == autoUpdateBtn) {
            SimplePreferences.addSettingsElement("autoUpdate", String.valueOf(autoUpdateBtn.isSelected()));
        }
        else if (e.getTarget() == alwaysOnTopBtn) {
            SimplePreferences.addSettingsElement("alwaysOnTop", String.valueOf(alwaysOnTopBtn.isSelected()));
            getMainStage().setAlwaysOnTop(alwaysOnTopBtn.isSelected());
        }
        else if (e.getTarget() == force_english_btn) {
            SimplePreferences.addSettingsElement("force_english", String.valueOf(force_english_btn.isSelected()));
            boolean restart =
                    showConfirmationScreen(LangHelper.getLang().getString("restart_necessary"), LangHelper.getLang()
                            .getString("restart_necessary_description"));
            if (restart) {
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

    @SuppressWarnings("unchecked")
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
        log.debug("Runechanger is located in: " + PathUtils.getWorkingDirectory());
        SimplePreferences.load();
        loadPreferences();
        settingsPane.setVisible(true);
        if (SimplePreferences.getSettingsValue("alwaysOnTop").equalsIgnoreCase("true")) {
            getMainStage().setAlwaysOnTop(true);
        }
        currentPane = settingsPane;
        FadeTransition fadeTransition = fade(mainPane, 400, 0, 1);
        fadeTransition.playFromStart();
    }

    private void setupPreference(String key, String defaultValue, JFXToggleButton button) {
        if (SimplePreferences.getSettingsValue(key) == null) {
            SimplePreferences.addSettingsElement(key, defaultValue);
        }
        if (SimplePreferences.getSettingsValue(key).equals("true")) {
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
        return dialogResult == JOptionPane.YES_OPTION;
    }

    public void init(Settings settings) {
        this.settings = settings;
    }

    public void debug() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext(); ) {
                Appender<ILoggingEvent> appender = index.next();
                if (appender instanceof FileAppender) {
                    try {
                        byte[] encoded =
                                Files.readAllBytes(Paths.get(((FileAppender<ILoggingEvent>) appender).getFile()));
                        String code = new LogRequest(new String(encoded)).submit();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(LangHelper.getLang().getString("logs_sent"));
                        alert.setHeaderText(null);
                        alert.setContentText(
                                String.format(LangHelper.getLang().getString("logs_sent_msg"), code));
                        ButtonType btn = new ButtonType(LangHelper.getLang().getString("copy_code"));
                        alert.getButtonTypes().add(btn);
                        Optional<ButtonType> buttonType = alert.showAndWait();
                        if (buttonType.isPresent() && buttonType.get() == btn) {
                            StringSelection stringSelection = new StringSelection(code);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(stringSelection, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
