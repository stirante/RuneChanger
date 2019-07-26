package com.stirante.RuneChanger.gui.controllers;

import com.stirante.RuneChanger.gui.Constants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class ContentAreaController implements Initializable {

    @FXML
    private MediaView mediaBackground;

    boolean flag = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Content Area Controller initializing");
        setVideoLoop();
    }

    @FXML
    private void open_sidebar(ActionEvent event) throws IOException {
        BorderPane border_pane = (BorderPane) ((Node) event.getSource()).getScene().getRoot();
        if (flag == true) {
            Parent sidebar = FXMLLoader.load(getClass().getResource("/fxml/Sidebar.fxml"));
            border_pane.setLeft(sidebar);
            BorderPane.setAlignment(sidebar, Pos.CENTER_LEFT);
            flag = false;
        } else {
            border_pane.setLeft(null);
            flag = true;
        }
    }

    private void setVideoLoop() {
        Media video = new Media(getClass().getResource("/images/background_ambient.mp4").toString());
        MediaPlayer player = new MediaPlayer(video);
        mediaBackground.setMediaPlayer(player);
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
