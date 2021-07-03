package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.components.Button;
import com.stirante.runechanger.util.LangHelper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import java.io.IOException;

public class SettingsButtonController {
    private final Pane root;

    @FXML
    private Button button;
    @FXML
    private Line separator;

    public SettingsButtonController(EventHandler<ActionEvent> onClick, String text, String description, boolean hideSeparator) {
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/SettingsButton.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        button.setText(text);
        button.tooltipProperty().setValue(new Tooltip(description));
        button.setOnAction(onClick);
        separator.setVisible(!hideSeparator);
    }

    public Pane getRoot() {
        return root;
    }
}
