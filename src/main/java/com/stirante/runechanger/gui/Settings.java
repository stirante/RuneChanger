package com.stirante.runechanger.gui;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.controllers.DialogController;
import com.stirante.runechanger.gui.controllers.HomeController;
import com.stirante.runechanger.gui.controllers.MainController;
import com.stirante.runechanger.gui.controllers.RuneBookController;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.runestore.RuneStore;
import com.stirante.runechanger.util.AsyncTask;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.SimplePreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
                    instance.home.setOnline(
                            RuneChanger.getInstance().getChampionSelectionModule().getCurrentSummoner(),
                            RuneChanger.getInstance().getLootModule());
                    instance.updateRunes();
                } catch (Exception ignored) {
                }
            }
            else {
                instance.home.setOffline();
                instance.updateRunes();
            }
        });
    }

    public static void main(String[] args) {
        RuneChanger.main(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        runeChanger = RuneChanger.getInstance();
        mainStage = stage;

        controller = new MainController(stage);
        runebook = new RuneBookController();

        controller.setOnChampionSearch(champion -> {
            runebook.setChampion(champion);
            RuneStore.getRemoteRunes(champion, runebook.newRunes);
            controller.setFullContent(runebook.container);
        });

        home = new HomeController();
        setClientConnected(false);
        runeChanger.getRunesModule().addOnPageChangeListener(this::updateRunes);
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

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            boolean isRunebook = controller.fullContentPane.getChildren().contains(runebook.container) &&
                    controller.contentPane.getChildren().isEmpty();
            boolean isHome = controller.fullContentPane.getChildren().isEmpty() &&
                    controller.contentPane.getChildren().contains(home.container);
            if (event.getCode() == KeyCode.C && event.isControlDown()) {
                if (isRunebook) {
                    copyRunePage(runebook.localRunesList.getSelectionModel().getSelectedItem());
                }
                else if (isHome) {
                    copyRunePage(home.localRunesList.getSelectionModel().getSelectedItem());
                }
            }
            else if (event.getCode() == KeyCode.V && event.isControlDown()) {
                if (isRunebook) {
                    pasteRunePage(Champion.getByName(runebook.championName.getText()));
                }
                else if (isHome) {
                    pasteRunePage(null);
                }
            }
        });
    }

    private void copyRunePage(RunePage runePage) {
        if (runePage != null) {
            StringSelection selection = new StringSelection(runePage.toSerializedString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        }
        runeChanger.getGuiHandler().showInfoMessage(LangHelper.getLang().getString("successful_rune_copy"));
    }

    private void pasteRunePage(Champion champion) {
        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data == null || data.isEmpty()) {
                return;
            }
            RunePage page = RunePage.fromSerializedString(data);
            if (page != null) {
                if (champion != null) {
                    page.setChampion(champion);
                }
                if (SimplePreferences.getRuneBookPage(page.getName()) != null) {
                    runeChanger.getGuiHandler().showWarningMessage(String.format(LangHelper.getLang()
                            .getString("duplicate_name_msg"), page.getName()));
                    return;
                }
                SimplePreferences.addRuneBookPage(page);
                runeChanger.getGuiHandler().showInfoMessage(LangHelper.getLang().getString("successful_rune_copy"));
            }
            else {
                runeChanger.getGuiHandler().showWarningMessage(LangHelper.getLang()
                        .getString("invalid_runepage"));
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRunes() {
        new AsyncTask<Void, Void, List<RunePage>>() {
            @Override
            public List<RunePage> doInBackground(Void[] params) {
                if (!runeChanger.getApi().isConnected()) {
                    return null;
                }
                try {
                    return runeChanger.getRunesModule().getRunePages();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(List<RunePage> result) {
                home.localRunes.clear();
                runebook.localRunes.clear();
                String title;
                ArrayList<RunePage> runeBookValues;
                if (result == null) {
                    title = LangHelper.getLang().getString("local_runes_no_connection");
                    runeBookValues = SimplePreferences.getRuneBookValues();
                }
                else {
                    title = String.format(
                            LangHelper.getLang().getString("local_runes"),
                            result.size(),
                            runeChanger.getRunesModule().getOwnedPageCount());
                    // Split list into runepages, that are both in runebook and in client and those, that are only in runebook
                    Map<Boolean, List<RunePage>> results =
                            SimplePreferences.getRuneBookValues()
                                    .stream()
                                    .collect(Collectors.partitioningBy(runePage -> result
                                            .stream().noneMatch(runePage1 -> runePage1.equals(runePage))));
                    for (RunePage page : result.stream()
                            .filter(p -> results.get(false)
                                    .stream()
                                    .anyMatch(runePage -> runePage.getName().equals(p.getName())))
                            .collect(Collectors.toCollection(ArrayList::new))) {
                        page.setSynced(true);
                    }
                    runeBookValues = new ArrayList<>(result);
                    runeBookValues.addAll(results.get(true));
                }
                home.localRunesTitle.setText(title);
                runebook.localRunesTitle.setText(title);
                home.localRunes.addAll(runeBookValues);
                runebook.localRunes.addAll(runeBookValues);
            }
        }.execute();
    }
}
