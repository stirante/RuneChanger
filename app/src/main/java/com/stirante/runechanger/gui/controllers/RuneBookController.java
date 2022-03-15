package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.gui.Content;
import com.stirante.runechanger.utils.SyncingListWrapper;
import generated.Position;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;

public class RuneBookController implements Content {
    private final RuneChangerApi api;
    public ImageView background;
    public ImageView position;
    public Label championName;
    public Label joke;
    public Label localRunesTitle;
    public ListView<ChampionBuild> localRunesList;
    public ListView<ChampionBuild> newRunesList;
    public Pane container;

    public final SyncingListWrapper<ChampionBuild> localRunes = new SyncingListWrapper<>();
    public final SyncingListWrapper<ChampionBuild> newRunes = new SyncingListWrapper<>();

    public RuneBookController(RuneChangerApi api) {
        this.api = api;
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("/fxml/RuneBook.fxml"), com.stirante.runechanger.RuneChanger.getInstance()
                        .getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        localRunesList.setItems(localRunes.getBackingList());
        newRunesList.setItems(newRunes.getBackingList());
        localRunes.addListener(observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    localRunes.sort(Comparator.comparing(ChampionBuild::getName));
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
        newRunes.addListener(observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    newRunes.sort(Comparator.comparing(ChampionBuild::getName));
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
        localRunesList.setCellFactory(listView -> new RuneItemController.ChampionBuildCell(api, RuneItemController::setLocalRuneMode));
        newRunesList.setCellFactory(listView -> new RuneItemController.ChampionBuildCell(api, RuneItemController::setNewRuneMode));
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
        BufferedImage splashArt = champion.getSplashArt();
        if (splashArt != null) {
            background.setImage(SwingFXUtils.toFXImage(splashArt, null));
            splashArt.flush();
        }
        else {
            background.setImage(null);
        }
        championName.setText(champion.getName());
        joke.setText(champion.getPickQuote());
        setPosition(champion.getPosition());
        FilteredList<ChampionBuild> filteredList = new FilteredList<>(localRunes.getBackingList(), runePage ->
                runePage.getRunePage().isFromClient() ||
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
        filteredList.addListener((ListChangeListener<ChampionBuild>) observable -> {
            if (filteredList.size() > 4) {
                localRunesList.setPrefWidth(282);
            }
            else {
                localRunesList.setPrefWidth(272);
            }
        });
    }

    public void onDetach() {
        background.setImage(null);
    }

    @Override
    public Node getNode() {
        return container;
    }
}
