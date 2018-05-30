package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.util.SimplePreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;

public class Settings extends Application {

    private static Stage stage;

    public static void initialize() {
        new Thread(Application::launch).start();
    }

    public static void show() {
        Platform.runLater(() -> stage.show());
    }

    @Override
    public void start(Stage stage) throws IOException {
        Platform.setImplicitExit(false);
        Settings.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Settings.class.getResource("/Settings.fxml"));
        BorderPane node = fxmlLoader.load();
        SettingsController controller = fxmlLoader.getController();
        final WebEngine engine = controller.getWebview().getEngine();
        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("app", this);
                engine.executeScript("init()");
            }
        });
        engine.load(Settings.class.getResource("/settings.html").toString());
        Scene scene = new Scene(node, 1280, 720);
        stage.setScene(scene);
        stage.setTitle("RuneChanger Settings");
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
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

    public static void main(String[] args) {
        SimplePreferences.load();
        initialize();
        show();
    }

}
