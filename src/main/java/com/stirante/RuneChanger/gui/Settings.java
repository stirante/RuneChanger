package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.InGameButton;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import com.stirante.RuneChanger.util.SimplePreferences;
import generated.LolLootPlayerLoot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Settings extends Application {

	public static Stage mainStage;
	private double xOffset = 0;
	private double yOffset = 0;
    private static Stage stage;
    private WebEngine engine;

    public static void initialize() {
        new Thread(Application::launch).start();
    }

    public static void show() {
		System.out.println("mainstage is showing: " + mainStage.isShowing());
		if (!mainStage.isShowing())
		{
			Platform.runLater(() -> {
				mainStage.show();
			});
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
//        SimplePreferences.load();
//        initialize();
//        show();
		launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
    	mainStage = stage;
		Parent root = FXMLLoader.load(getClass().getResource("/Settings.fxml"));
		stage.initStyle(StageStyle.TRANSPARENT);
		root.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				xOffset = event.getSceneX();
				yOffset = event.getSceneY();
			}
		});
		root.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				stage.setX(event.getScreenX() - xOffset);
				stage.setY(event.getScreenY() - yOffset);
			}
		});
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		Scene scene = new Scene(root);
		scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
		stage.setScene(scene);

//set Stage boundaries to the lower right corner of the visible bounds of the main screen
		stage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 385); //framesize -15
		stage.setY(primaryScreenBounds.getMinY() + primaryScreenBounds.getHeight() - 471); //framesize +5
		stage.setWidth(370);
		stage.setHeight(466);
		Platform.setImplicitExit(false);
		stage.show();
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setValue(String key, String value) {
        SimplePreferences.putValue(key, value);
    }

    public String getValue(String key) {
        return SimplePreferences.getValue(key);
    }

    public static void craftKeys() {
        new Thread(() -> {
            try {
                LolLootPlayerLoot keyFragments = InGameButton.getApi()
                        .executeGet("/lol-loot/v1/player-loot/MATERIAL_key_fragment", LolLootPlayerLoot.class);
                if (keyFragments.count >= 3) {
                    InGameButton.getApi()
                            .executePost("/lol-loot/v1/recipes/MATERIAL_key_fragment_forge/craft?repeat=" +
                                    keyFragments.count / 3, new String[]{"MATERIAL_key_fragment"});
                } else {
					Platform.runLater(() -> {
						showWarning("ERROR", "Not enough key fragments", "You do not have enough key fragments to perform this operation!");
					});
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
                        InGameButton.getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class);
                for (LolLootPlayerLoot item : loot) {
                    if (item.lootId.startsWith("CHAMPION_RENTAL_")) {
                        for (int i = 0; i < item.count; i++) {
                            InGameButton.getApi()
                                    .executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
