package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.client.Loot;
import com.stirante.RuneChanger.gui.controllers.MainController;
import com.stirante.RuneChanger.util.LangHelper;
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
import java.util.Arrays;

import static com.stirante.RuneChanger.gui.SettingsController.showWarning;

public class Settings extends Application {

    public static Stage mainStage;
    private double xOffset = 0;
    private double yOffset = 0;
    private RuneChanger runeChanger;
    private Loot lootModule;

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

    public void craftKeys() {
        new Thread(() -> {
            if (lootModule.craftKeys() == 0) {
                Platform.runLater(() -> showWarning("ERROR", LangHelper.getLang()
                        .getString("not_enough_key_fragments"), LangHelper.getLang()
                        .getString("not_enough_key_fragments_message")));
            }
        }).start();
    }

    public void disenchantChampions() {
        new Thread(() -> lootModule.disenchantChampions()).start();
    }

    @Override
    public void start(Stage stage) throws IOException {
        runeChanger = RuneChanger.getInstance();
        lootModule = new Loot(runeChanger.getApi());
        Font.loadFont(getClass().getResource("/Beaufort-Bold.ttf").toExternalForm(), 10);
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(LangHelper.getLang());
        fxmlLoader.setLocation(getClass().getResource("/fxml/ContentArea.fxml"));
        Parent root = fxmlLoader.load();
        ((MainController) fxmlLoader.getController()).init(this);
        stage.initStyle(StageStyle.TRANSPARENT);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
        scene.setFill(null);
        stage.setScene(scene);
        stage.setTitle(Constants.APP_NAME);

        //set Stage to the middle of the screen
        stage.setX((primaryScreenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((primaryScreenBounds.getHeight() - stage.getHeight()) / 2);

        Platform.setImplicitExit(false);

        if (!Arrays.asList(runeChanger.programArguments).contains("-minimized")) {
            stage.show();
        }
    }
}
