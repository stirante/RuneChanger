package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.LangHelper;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;

public class SettingsEditItemController {
    private static final Logger log = LoggerFactory.getLogger(SettingsEditItemController.class);

    private final Pane root;

    @FXML
    private CheckBox checkbox;
    @FXML
    private TextArea text;
    @FXML
    private Label title;
    @FXML
    private Label description;
    @FXML
    private Line separator;

    public SettingsEditItemController(String text, Property<Boolean> selected, String title, String description, Predicate<String> onChange, boolean hideSeparator) {
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/SettingsEditItem.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (selected != null) {
            checkbox.setVisible(true);
            checkbox.selectedProperty().setValue(selected.getValue());
            checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> selected.setValue(newValue));
            this.text.disableProperty().bind(checkbox.selectedProperty().not());
        }
        else {
            checkbox.setVisible(false);
        }
        this.text.textProperty().setValue(text);
        this.text.textProperty().addListener(FxUtils.delayedChangedListener((observable, oldValue, newValue) ->
                onChange == null || onChange.test(newValue)));
        this.title.setText(title);
        if (description != null && !description.isEmpty()) {
            this.description.setText("(" + description + ")");
        }
        separator.setVisible(!hideSeparator);
    }

    public Pane getRoot() {
        return root;
    }
}
