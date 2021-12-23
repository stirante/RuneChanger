package com.stirante.runechanger.updater;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.update4j.Configuration;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().isEmpty()) {
            logger.error("No update file specified!");
            System.exit(0);
        }
        Configuration configuration = null;
        try (Reader reader = new InputStreamReader(new FileInputStream(getParameters().getRaw().get(0)))) {
            configuration = Configuration.read(reader);
        } catch (Exception e) {
            logger.error("Failed to read update file!", e);
            System.exit(0);
        }
        stage.setTitle("RuneChanger Updater");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.getIcons()
                .addAll(
                        new Image(getClass().getResource("/images/32.png").toExternalForm()),
                        new Image(getClass().getResource("/images/256.png").toExternalForm()),
                        new Image(getClass().getResource("/images/48.png").toExternalForm())
                );
        StackPane root = null;
        MainController mainController = new MainController();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(mainController);
            loader.setLocation(Main.class.getResource("/fxml/Updater.fxml"));
            root = loader.load();
        } catch (Exception e) {
            logger.error("Failed to load fxml!", e);
            System.exit(0);
        }
        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
        Configuration finalConfiguration = configuration;
        new Thread(() -> {
            finalConfiguration.update(mainController);
        }
        ).start();
    }
}
