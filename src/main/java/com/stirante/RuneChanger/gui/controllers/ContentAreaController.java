package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXButton;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.gui.Settings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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

    @FXML
    private HBox titleBar;

    private boolean flag = false;
    private Parent sidebar;
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Content Area Controller initializing");
        ControllerUtil.getInstance().setContentPane(contentPane);
        makeStageDrageable(titleBar);
        sidebar = null;
        try {
            sidebar = ControllerUtil.getInstance().getLoader("/fxml/Sidebar.fxml").load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ControllerUtil.getInstance().getMainPane().setLeft(sidebar);
        BorderPane.setAlignment(sidebar, Pos.CENTER_LEFT);
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
    private void open_sidebar(ActionEvent event) {
        if (flag == true) {
            sidebar.setVisible(true);
            flag = false;
        }
        else {
            sidebar.setVisible(false);
            flag = true;
        }
    }

    private <T> void makeStageDrageable(T t) {
        Node node = (Node) t;
        node.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        node.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Settings.mainStage.setX(event.getScreenX() - xOffset);
                Settings.mainStage.setY(event.getScreenY() - yOffset);
            }
        });

    }
}
