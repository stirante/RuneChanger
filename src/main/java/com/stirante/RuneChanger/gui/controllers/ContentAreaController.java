package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXButton;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.gui.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    @FXML
    private JFXButton minimizeButton;

    @FXML
    private JFXButton shutdownButton;

    @FXML
    private BorderPane contentPane;

    boolean flag = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Content Area Controller initializing");
        ControllerUtil.getInstance().setContentPane(contentPane);
    }

    @FXML
    void handleMenuBarButtonPressed(ActionEvent event) {
        JFXButton target = (JFXButton) event.getTarget();
        if (target.equals(minimizeButton)) {
            Settings.mainStage.hide();
        }
        else if (target.equals(shutdownButton)) {
            log.warn("User requested program shutdown");
            System.exit(0);
        }
    }

    @FXML
    private void open_sidebar(ActionEvent event) throws IOException {
        BorderPane border_pane = (BorderPane) ((Node) event.getSource()).getScene().getRoot();
        if (flag == true) {
            Parent sidebar = ControllerUtil.getInstance().getLoader("/fxml/Sidebar.fxml").load();
            border_pane.setLeft(sidebar);
            BorderPane.setAlignment(sidebar, Pos.CENTER_LEFT);
            flag = false;
        }
        else {
            border_pane.setLeft(null);
            flag = true;
        }
    }
}
