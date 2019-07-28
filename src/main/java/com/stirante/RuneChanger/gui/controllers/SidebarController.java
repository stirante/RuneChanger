package com.stirante.RuneChanger.gui.controllers;

import com.stirante.RuneChanger.gui.ControllerUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class SidebarController implements Initializable {

    @FXML
    private MediaView backgroundMediaView;

    @FXML
    private HBox settingsButton;

    @FXML
    private HBox runebookButton;

    private BorderPane contentPane;
    private HBox currentContentShown = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Sidebar Controller initializing");
        contentPane = ControllerUtil.getInstance().getContentPane();
        setVideoLoop();
    }

    @FXML
    void handleSideBarButtonPressed(MouseEvent event) throws IOException {
        HBox pressedHBox = (HBox) event.getSource();
        if (pressedHBox.equals(currentContentShown)) {
            return;
        }

        if (settingsButton.equals(pressedHBox)) {
            Parent settings = ControllerUtil.getInstance().getLoader("/fxml/Settings.fxml").load();
            contentPane.setCenter(settings);
            ControllerUtil.getInstance().fade(settings, 700, 0, 1).playFromStart();
            currentContentShown = pressedHBox;
        }
        else if (runebookButton.equals(pressedHBox)) {

        }
    }

    private void setVideoLoop() {
        Media video = new Media(getClass().getResource("/images/background_ambient.mp4").toString());
        MediaPlayer player = new MediaPlayer(video);
        backgroundMediaView.setMediaPlayer(player);
        player.play();
        player.setOnEndOfMedia(new Runnable() {
            @Override
            public void run() {
                player.seek(Duration.ZERO);
                player.play();
            }
        });
    }
}
