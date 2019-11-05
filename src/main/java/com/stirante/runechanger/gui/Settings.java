package com.stirante.runechanger.gui;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.controllers.DialogController;
import com.stirante.runechanger.gui.controllers.HomeController;
import com.stirante.runechanger.gui.controllers.MainController;
import com.stirante.runechanger.gui.controllers.RuneBookController;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.runestore.RuneStore;
import com.stirante.runechanger.runestore.RuneforgeSource;
import com.stirante.runechanger.util.AsyncTask;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Settings extends Application {

    private static Settings instance;
    private Stage mainStage;
    private RuneChanger runeChanger;
    private MainController controller;
    private RuneBookController runebook;
    private HomeController home;

    public static void initialize() {
        new Thread(Application::launch).start();
    }

    public static void show() {
        if (!instance.mainStage.isShowing()) {
            Platform.runLater(() -> instance.mainStage.show());
        }
    }

    public static void toggle() {
        Platform.runLater(() -> {
            if (instance.mainStage.isShowing()) {
                instance.mainStage.hide();
            }
            else {
                instance.mainStage.show();
            }
        });
    }

    public static boolean openYesNoDialog(String title, String message) {
        DialogController dialog = new DialogController();
        dialog.setContentText(message);
        dialog.setTitle(title);
        return dialog.showAndWait().orElse(false);
    }

    public static void setClientConnected(boolean value) {
        Platform.runLater(() -> {
            if (value) {
                try {
                    instance.home.localRunes.clear();
                    instance.home.localRunes.addAll(SimplePreferences.getRuneBookValues());
                    instance.home.setOnline(RuneChanger.getInstance()
                            .getChampionSelectionModule()
                            .getCurrentSummoner(), RuneChanger.getInstance().getLootModule());
                    if (instance.runeChanger.getRunesModule() != null) {
                        new AsyncTask<Void, Void, Collection<RunePage>>() {
                            @Override
                            public Collection<RunePage> doInBackground(Void[] params) {
                                return instance.runeChanger.getRunesModule().getRunePages().values();
                            }

                            @Override
                            public void onPostExecute(Collection<RunePage> result) {
                                instance.home.localRunes.addAll(result);
                            }
                        }.execute();
                    }
                } catch (Exception ignored) {
                }
            }
            else {
                instance.home.setOffline();
                instance.home.localRunes.clear();
                instance.home.localRunes.addAll(SimplePreferences.getRuneBookValues());
            }
        });
    }

    public static void main(String[] args) {
        RuneChanger.main(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        RuneforgeSource source = new RuneforgeSource();
        runeChanger = RuneChanger.getInstance();
        mainStage = stage;

        controller = new MainController(stage);
        runebook = new RuneBookController();

        controller.setOnChampionSearch(champion -> {
            runebook.localRunes.clear();
            runebook.newRunes.clear();
            RuneStore.getRemoteRunes(champion, runebook.newRunes);
            if (runeChanger.getRunesModule() != null) {
                new AsyncTask<Void, Void, Collection<RunePage>>() {
                    @Override
                    public Collection<RunePage> doInBackground(Void[] params) {
                        return runeChanger.getRunesModule().getRunePages().values();
                    }

                    @Override
                    public void onPostExecute(Collection<RunePage> result) {
                        runebook.localRunes.addAll(result);
                    }
                }.execute();
            }
            runebook.localRunes.addAll(SimplePreferences.getRuneBookValues());
            if (champion.getSplashArt() != null) {
                runebook.background.setImage(SwingFXUtils.toFXImage(champion.getSplashArt(), null));
            }
            else {
                runebook.background.setImage(null);
            }
            runebook.championName.setText(champion.getName());
            runebook.setPosition(champion.getPosition());
            controller.setFullContent(runebook.container);
        });

        home = new HomeController();
        home.setOffline();
        controller.setContent(home.container);

        Scene scene = new Scene(controller.container, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.setFill(null);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle(Constants.APP_NAME);
        stage.getIcons()
                .add(new Image(getClass().getResource("/images/runechanger-runeforge-icon-32x32.png")
                        .toExternalForm()));

        Platform.setImplicitExit(false);

        if (!Arrays.asList(runeChanger.programArguments).contains("-minimized")) {
            stage.show();
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
        }
    }
}
