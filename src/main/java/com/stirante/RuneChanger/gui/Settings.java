package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.util.LangHelper;
import generated.LolLootPlayerLoot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

import static com.stirante.RuneChanger.gui.SettingsController.showWarning;

public class Settings extends Application {

    public static Stage mainStage;
    private double xOffset = 0;
    private double yOffset = 0;

    public static void initialize() {
        new Thread(Application::launch).start();
    }

    public static void show() {
        if (!mainStage.isShowing()) {
            Platform.runLater(() -> mainStage.show());
        }
    }

    public static void toggle() {
        Platform.runLater(() -> {
            if (mainStage.isShowing()) {
                mainStage.hide();
            }
            else {
                mainStage.show();
            }
        });
    }

    public static void main(String[] args) {
        RuneChanger.main(args);
    }

    public static void craftKeys() {
        new Thread(() -> {
            try {
                LolLootPlayerLoot keyFragments = RuneChanger.getApi()
                        .executeGet("/lol-loot/v1/player-loot/MATERIAL_key_fragment", LolLootPlayerLoot.class);
                if (keyFragments.count >= 3) {
                    RuneChanger.getApi()
                            .executePost("/lol-loot/v1/recipes/MATERIAL_key_fragment_forge/craft?repeat=" +
                                    keyFragments.count / 3, new String[]{"MATERIAL_key_fragment"});
                }
                else {
                    Platform.runLater(() -> showWarning("ERROR", LangHelper.getLang()
                            .getString("not_enough_key_fragments"), LangHelper.getLang()
                            .getString("not_enough_key_fragments_message")));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void disenchantChampions() {
        new Thread(() -> {
            try {
                LolLootPlayerLoot[] loot =
                        RuneChanger.getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class);
                for (LolLootPlayerLoot item : loot) {
                    if (item.lootId.startsWith("CHAMPION_RENTAL_")) {
                        for (int i = 0; i < item.count; i++) {
                            RuneChanger.getApi()
                                    .executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResource("/Beaufort-Bold.ttf").toExternalForm(), 10);
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(LangHelper.getLang());
        fxmlLoader.setLocation(getClass().getResource("/Settings.fxml"));
        Parent root = fxmlLoader.load();
        stage.initStyle(StageStyle.TRANSPARENT);
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.setFill(null);
        stage.setScene(scene);

        //set Stage boundaries to the lower right corner of the visible bounds of the main screen
        stage.setHeight(470);
        stage.setWidth(480);
        stage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - stage.getWidth());
        stage.setY(primaryScreenBounds.getMinY() + primaryScreenBounds.getHeight() - stage.getHeight());
        Platform.setImplicitExit(false);
        stage.show();
    }

}
