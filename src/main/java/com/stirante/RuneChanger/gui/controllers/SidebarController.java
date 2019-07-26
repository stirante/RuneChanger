package com.stirante.RuneChanger.gui.controllers;

import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class SidebarController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Sidebar Controller initializing");
    }

    private void setVideoLoop() {
        Media video = new Media(getClass().getResource("/images/background_ambient.mp4").toString());
        MediaPlayer player = new MediaPlayer(video);
        media.setMediaPlayer(player);
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
