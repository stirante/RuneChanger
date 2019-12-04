package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.LangHelper;
import generated.Position;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.Comparator;

public class RuneBookController {
    public ImageView background;
    public ImageView position;
    public Label championName;
    public Label joke;
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
        localRunes.addListener((ListChangeListener<RunePage>) observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    FXCollections.sort(localRunes, Comparator.comparing(RunePage::getName));
                    return;
                }
            }
            if (localRunes.size() > 4) {
                localRunesList.setPrefWidth(282);
            }
            else {
                localRunesList.setPrefWidth(272);
            }
        });
        newRunes.addListener((ListChangeListener<RunePage>) observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    FXCollections.sort(newRunes, Comparator.comparing(RunePage::getName));
                    return;
                }
            }
            if (newRunes.size() > 4) {
                newRunesList.setPrefWidth(282);
            }
            else {
                newRunesList.setPrefWidth(272);
            }
        });
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
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-top.png")
                        .toExternalForm()));
                break;
            case MIDDLE:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-middle.png")
                        .toExternalForm()));
                break;
            case BOTTOM:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-bottom.png")
                        .toExternalForm()));
                break;
            case UTILITY:
                this.position.setImage(new Image(getClass().getResource("/images/icon-position-utility.png")
                        .toExternalForm()));
                break;
            default:
                this.position.setVisible(false);
                break;
        }
    }

    public void setChampion(Champion champion) {
        newRunes.clear();
        if (champion.getSplashArt() != null) {
            background.setImage(SwingFXUtils.toFXImage(champion.getSplashArt(), null));
        }
        else {
            background.setImage(null);
        }
        championName.setText(champion.getName());
        joke.setText(champion.getPickQuote());
        setPosition(champion.getPosition());
        FilteredList<RunePage> filteredList = new FilteredList<>(localRunes, runePage ->
                runePage.isFromClient() ||
                        runePage.getChampion() == null ||
                        runePage.getChampion().equals(champion)
        );
        localRunesList.setItems(filteredList);
        if (filteredList.size() > 4) {
            localRunesList.setPrefWidth(282);
        }
        else {
            localRunesList.setPrefWidth(272);
        }
        filteredList.addListener((ListChangeListener<RunePage>) observable -> {
            if (filteredList.size() > 4) {
                localRunesList.setPrefWidth(282);
            }
            else {
                localRunesList.setPrefWidth(272);
            }
        });
    }
}