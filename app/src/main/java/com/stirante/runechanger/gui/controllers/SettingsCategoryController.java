package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.utils.LangHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SettingsCategoryController {
    private static final Logger log = LoggerFactory.getLogger(SettingsCategoryController.class);

    private final Pane root;
    private final String id;
    private final Runnable onClick;

    @FXML
    private Label name;
    @FXML
    private Line line;

    public SettingsCategoryController(String id, String name, Runnable onSelect) {
        this.id = id;
        this.onClick = onSelect;
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/SettingsCategory.fxml"), com.stirante.runechanger.RuneChanger.getInstance()
                        .getLang());
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.name.setText(name);
    }

    @FXML
    public void handleMouseClicked(MouseEvent e) {
        if (onClick != null && !line.isVisible()) {
            onClick.run();
        }
    }

    public void setSelected(boolean selected) {
        line.setVisible(selected);
    }

    public Pane getRoot() {
        return root;
    }

    public String getCategoryId() {
        return id;
    }
}
