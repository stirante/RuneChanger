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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class ContentAreaController implements Initializable {

    boolean flag = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Content Area Controller initializing");
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
}
