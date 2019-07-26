package com.stirante.RuneChanger.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class SidebarController implements Initializable {

    @FXML
    private MediaView backgroundMediaView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Sidebar Controller initializing");
        setVideoLoop();
    }

    private void setVideoLoop() {
        Media video = new Media(getClass().getResource("/images/sidebarBackground.mp4").toString());
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
