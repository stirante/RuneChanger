package com.stirante.RuneChanger.gui.controllers;

import com.stirante.RuneChanger.model.client.RunePage;
import com.stirante.RuneChanger.util.LangHelper;
import generated.Position;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class RuneBookController {
    public ImageView background;
    public ImageView position;
    public Label championName;
    public Label localRunesTitle;
    public ListView<RunePage> localRunesList;
    public ListView<RunePage> newRunesList;
    public Pane container;

    public ObservableList<RunePage> localRunes = FXCollections.observableArrayList();
    public ObservableList<RunePage> newRunes = FXCollections.observableArrayList();

    public RuneBookController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/RuneBook.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        localRunesList.setItems(localRunes);
        newRunesList.setItems(newRunes);
        localRunesList.setCellFactory(listView -> new RuneItemController.RunePageCell(RuneItemController::setLocalRuneMode));
        newRunesList.setCellFactory(listView -> new RuneItemController.RunePageCell(RuneItemController::setNewRuneMode));
    }

    public void onBuildsClick(ActionEvent actionEvent) {

    }

    public void setPosition(Position position) {
        this.position.setVisible(true);
        if (position == null) {
            this.position.setVisible(false);
            return;
        }
        switch (position) {
            case TOP:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-top.png").toExternalForm()));
                break;
            case MIDDLE:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-middle.png").toExternalForm()));
                break;
            case BOTTOM:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-bottom.png").toExternalForm()));
                break;
            case UTILITY:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-utility.png").toExternalForm()));
                break;
            default:
                this.position.setVisible(false);
                break;
        }
    }

}
