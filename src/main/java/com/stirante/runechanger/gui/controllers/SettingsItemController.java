package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.util.LangHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import java.io.IOException;
import java.util.function.Predicate;

public class SettingsItemController {
    private final Pane root;
    private boolean canceling = false;

    @FXML
    private CheckBox checkbox;
    @FXML
    private Label title;
    @FXML
    private Label description;
    @FXML
    private Line separator;

    public SettingsItemController(boolean selected, Predicate<Boolean> onChange, String title, String description, boolean hideSeparator) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SettingsItem.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkbox.setSelected(selected);
        this.title.setText(title);
        this.description.setText(description);
        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!canceling && !onChange.test(checkbox.isSelected())) {
                canceling = true;
                Platform.runLater(() -> {
                    checkbox.selectedProperty().setValue(oldValue);
                    canceling = false;
                });
            }
        });
        separator.setVisible(!hideSeparator);
    }

    public Pane getRoot() {
        return root;
    }
}
