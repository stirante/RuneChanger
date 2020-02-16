package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.util.LangHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

import java.io.IOException;

public class ProgressDialogController extends Dialog<Void> {
    public Pane container;
    public Label title;
    public ProgressBar progress;

    private double xOffset;
    private double yOffset;

    public ProgressDialogController() {
        setDialogPane(new CustomDialogPane());
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/ProgressDialog.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getDialogPane().setContent(container);
        title.textProperty().bind(titleProperty());
        container.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        getDialogPane().setStyle(
                "-fx-background-color: transparent;"
        );
        container.getScene().setFill(Color.TRANSPARENT);
        initStyle(StageStyle.TRANSPARENT);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
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

    public void setProgress(double value) {
        progress.setProgress(value);
    }

    public double getProgress() {
        return progress.getProgress();
    }

    public static class CustomDialogPane extends DialogPane {

        @Override
        protected Node createButton(ButtonType buttonType) {
            // Don't create any buttons. We actually have to add one button, so we will be able to close dialog without setting result
            return new Pane();
        }
    }
}
