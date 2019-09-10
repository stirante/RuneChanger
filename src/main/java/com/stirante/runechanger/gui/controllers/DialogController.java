package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.util.LangHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

import java.io.IOException;

public class DialogController extends Dialog<Boolean> {
    public Pane container;
    public Label title;
    public Label message;
    public Button noButton;
    public Button yesButton;

    private double xOffset;
    private double yOffset;

    public DialogController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/YesNoDialog.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getDialogPane().setContent(container);
        message.textProperty().bind(contentTextProperty());
        title.textProperty().bind(titleProperty());
        container.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        getDialogPane().setStyle(
                "-fx-background-color: transparent;"
        );
        container.getScene().setFill(Color.TRANSPARENT);
        initStyle(StageStyle.TRANSPARENT);
    }

    public void handleButtonPress(ActionEvent e) {
        setResult(e.getSource() == yesButton);
    }

    public void onHandlePress(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void onHandleDrag(MouseEvent event) {
        setX(event.getScreenX() - xOffset);
        setY(event.getScreenY() - yOffset);
    }

}
