package com.stirante.runechanger.gui;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.scene.control.Label;
import javafx.stage.Modality;

import java.util.concurrent.atomic.AtomicBoolean;

public class ControllerUtil {

    public static boolean showConfirmationScreen(String title, String body) {
        AtomicBoolean returnVal = new AtomicBoolean(false);
        JFXAlert alert = new JFXAlert(null);
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

    public static void showInfo(String title, String body) {
        JFXAlert alert = new JFXAlert(null);
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

}
