package com.stirante.RuneChanger.gui;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.stirante.RuneChanger.util.LangHelper;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

public class ControllerUtil {
    private final static ControllerUtil instance = new ControllerUtil();
    private BorderPane mainPane;
    private BorderPane contentPane;

    public static ControllerUtil getInstance() {
        return instance;
    }

    public FXMLLoader getLoader(String fxmlPath) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(LangHelper.getLang());
        fxmlLoader.setLocation(getClass().getResource(fxmlPath));
        return fxmlLoader;
    }

    public <T> FadeTransition fade(T node, int duration, int from, int to) {
        FadeTransition ft = new FadeTransition(Duration.millis(duration), (Node) node);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    public boolean showConfirmationScreen(String title, String body) {
        AtomicBoolean returnVal = new AtomicBoolean(false);
        JFXAlert alert = new JFXAlert(Settings.mainStage);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOverlayClose(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(new Label(title));
        layout.setBody(new Label(body));

        JFXButton yesButton = new JFXButton("Yes");
        yesButton.getStyleClass().add("dialog-accept");
        yesButton.focusTraversableProperty().setValue(false);
        yesButton.setOnAction(event -> {alert.hideWithAnimation(); returnVal.set(true);});

        JFXButton noButton = new JFXButton("No");
        noButton.getStyleClass().add("dialog-accept");
        noButton.focusTraversableProperty().setValue(false);
        noButton.setOnAction(event -> {alert.hideWithAnimation(); returnVal.set(false);});

        layout.setActions(yesButton, noButton);
        alert.setContent(layout);
        alert.showAndWait();
        return returnVal.get();
    }

    public void showInfo(String title, String body) {
        JFXAlert alert = new JFXAlert(Settings.mainStage);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOverlayClose(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(new Label(title));
        layout.setBody(new Label(body));

        JFXButton yesButton = new JFXButton("Ok");
        yesButton.getStyleClass().add("dialog-accept");
        yesButton.focusTraversableProperty().setValue(false);
        yesButton.setOnAction(event -> alert.hideWithAnimation());

        layout.setActions(yesButton);
        alert.setContent(layout);
        alert.showAndWait();
    }

    public BorderPane getMainPane() {
        return mainPane;
    }

    public void setMainPane(BorderPane p) {
        this.mainPane = p;
    }

    public BorderPane getContentPane() {
        return contentPane;
    }

    public void setContentPane(BorderPane p) {
        this.contentPane = p;
    }

}
