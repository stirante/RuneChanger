package com.stirante.runechanger.gui.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class SettingsComboBoxItemController<T> {
    private final Pane root;
    private boolean canceling = false;

    @FXML
    private ComboBox<T> comboBox;
    @FXML
    private Label title;
    @FXML
    private Label description;
    @FXML
    private Line separator;

    public SettingsComboBoxItemController(T selected, List<T> items, Predicate<T> onChange, Function<T, String> nameFunction, String title, String description, boolean hideSeparator) {
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/SettingsComboBoxItem.fxml"), com.stirante.runechanger.RuneChanger.getInstance()
                        .getLang());
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, T> map = new HashMap<>();
        for (T item : items) {
            map.put(nameFunction.apply(item), item);
        }
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return nameFunction.apply(object);
            }

            @Override
            public T fromString(String string) {
                return map.get(string);
            }
        });
        comboBox.setItems(FXCollections.observableList(items));
        comboBox.getSelectionModel().select(selected);
        this.title.setText(title);
        this.description.setText(description);
        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!canceling && !onChange.test(newValue)) {
                canceling = true;
                Platform.runLater(() -> {
                    comboBox.getSelectionModel().select(oldValue);
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
