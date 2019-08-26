package com.stirante.RuneChanger.gui.controllers;

import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.model.client.RunePage;
import com.stirante.RuneChanger.util.LangHelper;
import generated.LolSummonerSummoner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
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

public class HomeController {

    public Pane container;
    public Circle profilePicture;
    public ImageView emptyProfilePicture;
    public Label username;
    public Label localRunesTitle;
    public ListView<RunePage> localRunesList;

    public ObservableList<RunePage> localRunes = FXCollections.observableArrayList();

    public HomeController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        localRunesList.setItems(localRunes);
        localRunesList.setCellFactory(listView -> new RuneItemController.RunePageCell(RuneItemController::setHomeRuneMode));
    }

    public void setOnline(LolSummonerSummoner summoner) {
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
    }

    public void setOffline() {
        emptyProfilePicture.setVisible(true);
        username.setText("");
        profilePicture.setFill(null);
    }

    public void onChampionDisenchant(ActionEvent event) {
        RuneChanger.getInstance().getLootModule().disenchantChampions();
    }

    public void onCraftKeys(ActionEvent event) {
        RuneChanger.getInstance().getLootModule().craftKeys();
    }

}
