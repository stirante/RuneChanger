package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.components.Button;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.LangHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.io.IOException;

public class DialogController extends Dialog<ButtonType> {
    public Pane container;
    public Label title;
    public Label message;
    public HBox buttonContainer;

    private double xOffset;
    private double yOffset;

    public DialogController() {
        setDialogPane(new CustomDialogPane());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CustomDialog.fxml"), LangHelper.getLang());
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

    @FXML
    public void onHandlePress(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    public void onHandleDrag(MouseEvent event) {
        setX(event.getScreenX() - xOffset);
        setY(event.getScreenY() - yOffset);
    }

    public void setButtonTypes(ButtonType[] buttons) {
        buttonContainer.getChildren().clear();
        for (ButtonType button : buttons) {
            Button btn = new Button();
            btn.setText(button.getText());
            btn.setWidth(120);
            btn.setHeight(30);
            buttonContainer.getChildren().add(btn);
            btn.setOnAction(event -> {
                setResult(button);
            });
            getDialogPane().getButtonTypes().clear();
            getDialogPane().getButtonTypes().addAll(buttons);
        }
    }

    public static class CustomDialogPane extends DialogPane {

        @Override
        protected Node createButton(ButtonType buttonType) {
            return new Pane();
        }
    }
}
