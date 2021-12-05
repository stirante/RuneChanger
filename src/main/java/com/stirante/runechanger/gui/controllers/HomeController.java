package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.Loot;
import com.stirante.runechanger.gui.Content;
import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.gui.components.RCButton;
import com.stirante.runechanger.model.client.ChampionBuild;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;
import generated.LolSummonerSummoner;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

public class HomeController implements Content {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    public Pane container;
    public Circle profilePicture;
    public ImageView emptyProfilePicture;
    public Label username;
    public Label localRunesTitle;
    public RCButton disenchantChampionsButton;
    public RCButton syncButton;
    public ListView<ChampionBuild> localRunesList;

    public final ObservableList<ChampionBuild> localRunes = FXCollections.observableArrayList();
    private Loot lootModule;

    public HomeController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        localRunes.addListener((ListChangeListener<ChampionBuild>) observable -> {
            while (observable.next()) {
                if (observable.getAddedSize() > 0 || observable.getRemovedSize() > 0) {
                    FXCollections.sort(localRunes, Comparator.comparing(ChampionBuild::getName));
                    return;
                }
            }
            if (localRunes.size() > 7) {
                localRunesList.setPrefWidth(282);
            }
            else {
                localRunesList.setPrefWidth(272);
            }
        });
        localRunesList.setItems(localRunes);
        localRunesList.setCellFactory(listView -> new RuneItemController.ChampionBuildCell(RuneItemController::setHomeRuneMode));
        syncButton.setVisible(!SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_SYNC, false));
        Objects.requireNonNull(syncButton.getTooltip()).setShowDelay(Duration.ZERO);
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
            log.error("Exception occurred while getting a profile icon", e);
        }
        emptyProfilePicture.setVisible(false);
        disenchantChampionsButton.setDisable(false);
        syncButton.setDisable(false);
    }

    public void setOffline() {
        emptyProfilePicture.setVisible(true);
        username.setText("");
        profilePicture.setFill(null);
        disenchantChampionsButton.setDisable(true);
        syncButton.setDisable(true);
        localRunesTitle.setText(LangHelper.getLang().getString("local_runes_no_connection"));
        username.setText(LangHelper.getLang().getString("not_connected"));
    }

    @FXML
    public void onChampionDisenchant(ActionEvent event) {
        if (Settings.openYesNoDialog(LangHelper.getLang().getString("disenchant_confirm_title"), LangHelper.getLang().getString("disenchant_confirm_message"))) {
            if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.SMART_DISENCHANT, false)) {
                lootModule.smartDisenchantChampions();
            }
            else {
                lootModule.disenchantChampions();
            }
        }
    }

    @FXML
    public void onSync(ActionEvent event) {
        RuneChanger.getInstance().getRunesModule().syncRunePages();
    }

    @Override
    public void onDetach() {

    }

    @Override
    public Node getNode() {
        return container;
    }
}
