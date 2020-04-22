package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.util.LangHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class SettingsItemController {
    private static final Logger log = LoggerFactory.getLogger(SettingsItemController.class);

    private final Consumer<Boolean> setter;

    private final Pane root;

    @FXML
    private CheckBox checkbox;
    @FXML
    private Label title;
    @FXML
    private Label description;

    public SettingsItemController(boolean selected, Consumer<Boolean> setter, String title, String description) {
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
        this.setter = setter;
    }

    @FXML
    void handleCheckboxPressed(ActionEvent e) {
        setter.accept(checkbox.isSelected());
    }

    public Pane getRoot() {
        return root;
    }
}
