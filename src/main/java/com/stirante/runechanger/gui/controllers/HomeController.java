package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.Loot;
import com.stirante.runechanger.gui.components.Button;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.LangHelper;
import generated.LolSummonerSummoner;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;

public class HomeController {

    public Pane container;
    public Circle profilePicture;
    public ImageView emptyProfilePicture;
    public Label username;
    public Label localRunesTitle;
    public Button disenchantChampionsButton;
    public ListView<RunePage> localRunesList;

    public ObservableList<RunePage> localRunes = FXCollections.observableArrayList();
    private Loot lootModule;

    public HomeController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        localRunes.addListener((ListChangeListener<RunePage>) observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    FXCollections.sort(localRunes, Comparator.comparing(RunePage::getName));
                    return;
                }
            }
        });
        localRunesList.setItems(localRunes);
        localRunesList.setCellFactory(listView -> new RuneItemController.RunePageCell(RuneItemController::setHomeRuneMode));
    }

    public void setOnline(LolSummonerSummoner summoner, Loot lootModule) {
        this.lootModule = lootModule;
        username.setText(summoner.displayName);
        try {
            BufferedImage profileIcon = ImageIO.read(RuneChanger.getInstance()
                    .getApi()
                    .getAsset("lol-game-data", "v1/profile-icons/" + summoner.profileIconId + ".jpg"));
            profilePicture.setFill(new ImagePattern(SwingFXUtils.toFXImage(profileIcon, null)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        emptyProfilePicture.setVisible(false);
        disenchantChampionsButton.setDisable(false);
    }

    public void setOffline() {
        emptyProfilePicture.setVisible(true);
        username.setText("");
        profilePicture.setFill(null);
        disenchantChampionsButton.setDisable(true);
        localRunesTitle.setText(LangHelper.getLang().getString("local_runes_no_connection"));
    }

    @FXML
    public void onChampionDisenchant(ActionEvent event) {
        //lootModule.disenchantChampions();
        System.out.println("click");
    }

}
