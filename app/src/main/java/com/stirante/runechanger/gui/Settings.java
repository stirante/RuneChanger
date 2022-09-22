package com.stirante.runechanger.gui;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.EventPriority;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RunePage;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.client.ClientModule;
import com.stirante.runechanger.client.Runes;
import com.stirante.runechanger.gui.controllers.*;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.util.AutoUpdater;
import com.stirante.runechanger.util.PerformanceMonitor;
import com.stirante.runechanger.utils.*;
import generated.LolGameflowGameflowPhase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import javafx.util.StringConverter;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Settings extends Application {
    private static final Logger log = LoggerFactory.getLogger(Settings.class);
    public static final String GAME_PHASE_EVENT = "/lol-gameflow/v1/gameflow-phase";

    private static Settings instance;
    private Stage mainStage;
    private RuneChanger api;
    private MainController controller;
    private RuneBookController runebook;
    private HomeController home;
    private final EventHandler<KeyEvent> keyPress = this::handleKeyPress;
    private boolean donateDontAsk = false;

    public static void initialize() {
        EventBus.register(Settings.class);
        AsyncTask.EXECUTOR_SERVICE.submit((Runnable) Application::launch);
    }

    public static void show() {
        if (instance.mainStage != null && !instance.mainStage.isShowing()) {
            Platform.runLater(() -> instance.createScene());
        }
    }

    public static void toggle() {
        Platform.runLater(() -> {
            if (instance.mainStage != null && instance.mainStage.isShowing()) {
                instance.destroyScene();
            }
            else {
                instance.createScene();
            }
        });
    }

    public static ButtonType openDialog(String title, String message, ButtonType... buttons) {
        DialogController dialog = new DialogController();
        dialog.setContentText(message);
        dialog.setTitle(title);
        dialog.setButtonTypes(buttons);
        return dialog.showAndWait().orElse(null);
    }

    public static boolean openYesNoDialog(String title, String message) {
        return openDialog(title, message, ButtonType.YES, ButtonType.NO) == ButtonType.YES;
    }

    public static void openOkDialog(String title, String message) {
        openDialog(title, message, ButtonType.OK);
    }

    @Subscribe(ClientModule.CURRENT_SUMMONER_EVENT)
    public static void onCurrentSummoner() {
        setClientConnected(true);
    }

    public static void setClientConnected(boolean value) {
        Platform.runLater(() -> {
            if (instance.home == null) {
                return;
            }
            if (value) {
                try {
                    instance.home.localRunes.clear();
                    instance.home.localRunes.addAll(instance.api.getRuneBook().getRuneBookValues());
                    instance.home.setOnline(
                            instance.api.getChampionSelectionModule().getCurrentSummoner(),
                            instance.api.getLootModule());
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

    private void destroyScene() {
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.GUI_HIDE);
        mainStage.hide();
        mainStage.setScene(null);
        runebook = null;
        controller = null;
        home = null;
        mainStage.removeEventHandler(KeyEvent.KEY_PRESSED, keyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        boolean isRunebook = controller.fullContentPane.getChildren().contains(runebook.container) &&
                controller.contentPane.getChildren().isEmpty();
        boolean isHome = controller.fullContentPane.getChildren().isEmpty() &&
                controller.contentPane.getChildren().contains(home.container);
        if (event.getCode() == KeyCode.C && event.isControlDown()) {
            if (isRunebook) {
                copyRunePage(runebook.localRunesList.getSelectionModel().getSelectedItem().getRunePage());
            }
            else if (isHome) {
                copyRunePage(home.localRunesList.getSelectionModel().getSelectedItem().getRunePage());
            }
        }
        else if (event.getCode() == KeyCode.V && event.isControlDown()) {
            if (isRunebook) {
                pasteRunePage(api.getChampions().getByName(runebook.championName.getText()));
            }
            else if (isHome) {
                pasteRunePage(null);
            }
        }
    }

    private void createScene() {
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.GUI_SHOW);
        controller = new MainController(api, mainStage);
        runebook = new RuneBookController(api);

        controller.setOnChampionSearch(champion -> {
            runebook.setChampion(champion);
            SourceStore.getRemoteRunes(champion, runebook.newRunes);
            controller.setFullContent(runebook);
        });

        home = new HomeController(api);
        setClientConnected(api.getClientApi() != null && api.getClientApi().isConnected());
        controller.setContent(home);

        Scene scene = new Scene(controller.container, 600, 500);
        String e = getClass().getResource("/style.css").toExternalForm();
        if (DebugConsts.ENABLE_STYLESHEET_DEBUG) {
            try {
                e = new File("app/src/main/resources/style.css").toURI().toURL().toExternalForm();
                final String f = e;
                final Path path = FileSystems.getDefault().getPath("app/src/main/resources/style.css");
                final WatchService watchService = FileSystems.getDefault().newWatchService();
                path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                new Thread(() -> {
                    while (true) {
                        try {
                            final WatchKey wk = watchService.take();
                            for (WatchEvent<?> event : wk.pollEvents()) {
                                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                    continue;
                                }
                                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                                final Path filename = ev.context();
                                if (path.endsWith(filename)) {
                                    Platform.runLater(() -> {
                                        log.debug("Reloading stylesheet");
                                        scene.getStylesheets().clear();
                                        scene.getStylesheets().add(f);
                                    });
                                }
                            }
                            if (!wk.reset()) {
                                break;
                            }
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }).start();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        scene.getStylesheets().add(e);
        scene.setFill(null);
        scene.setNodeOrientation(LangHelper.isTextRTL(instance.api.getLang()
                .getLocale()) ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        mainStage.setScene(scene);

        mainStage.addEventHandler(KeyEvent.KEY_PRESSED, keyPress);

        mainStage.show();
        mainStage.setAlwaysOnTop(true);
        mainStage.setAlwaysOnTop(false);
    }

    @Override
    public void start(Stage stage) {
        try {
            instance = this;
            api = RuneChanger.getInstance();
            mainStage = stage;

            mainStage.initStyle(StageStyle.TRANSPARENT);
            mainStage.setTitle(Constants.APP_NAME);
            mainStage.getIcons()
                    .addAll(
                            new Image(getClass().getResource("/images/32.png").toExternalForm()),
                            new Image(getClass().getResource("/images/256.png").toExternalForm()),
                            new Image(getClass().getResource("/images/48.png").toExternalForm())
                    );

            mainStage.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.D) {
                    if (PerformanceMonitor.isRunning()) {
                        ProgressDialogController progressDialog = new ProgressDialogController();
                        progressDialog.setTitle("Saving performance monitor output");
                        progressDialog.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        progressDialog.show();
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            public Void doInBackground(Void[] params) {
                                PerformanceMonitor.stop();
                                return null;
                            }

                            @Override
                            public void onPostExecute(Void result) {
                                progressDialog.close();
                                api
                                        .getGuiHandler()
                                        .showInfoMessage("Performance monitor output saved");
                            }
                        }.execute();
                    }
                    else {
                        PerformanceMonitor.start();
                        api.getGuiHandler().showInfoMessage("Performance monitor started");
                    }
                }
                else if (event.isControlDown() && event.getCode() == KeyCode.L) {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.setTitle("LCU connection debug");
                    alert.setHeaderText("Creating debug log of LCU connection");
                    alert.setContentText(null);
                    alert.setOnCloseRequest(event1 -> alert.close());


                    TextArea textArea = new TextArea("");
                    textArea.setEditable(false);
                    textArea.setWrapText(true);

                    textArea.setMaxWidth(Double.MAX_VALUE);
                    textArea.setMaxHeight(Double.MAX_VALUE);
                    GridPane.setVgrow(textArea, Priority.ALWAYS);
                    GridPane.setHgrow(textArea, Priority.ALWAYS);

                    GridPane expContent = new GridPane();
                    expContent.setMaxWidth(Double.MAX_VALUE);
                    expContent.add(textArea, 0, 1);

                    alert.getDialogPane().setContent(expContent);

                    alert.show();
                    new AsyncTask<Void, String, Void>() {
                        @Override
                        public Void doInBackground(Void[] params) {
                            ClientApi.generateDebugLog(this::publishProgress);
                            return null;
                        }

                        @Override
                        public void onProgress(String progress) {
                            super.onProgress(progress);
                            textArea.setText(textArea.getText() + "\n" + progress);
                            textArea.selectPositionCaret(textArea.getLength());
                            textArea.deselect();
                        }

                        @Override
                        public void onPostExecute(Void result) {
                            alert.setHeaderText("Done!");
                            ButtonType copy = new ButtonType("Copy");
                            alert.getButtonTypes().addAll(copy, ButtonType.OK);
                            alert.resultProperty().addListener((observable, oldValue, newValue) -> {
                                if (newValue == copy) {
                                    StringSelection selection = new StringSelection(textArea.getText());
                                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                                }
                                alert.close();
                            });
                        }
                    }.execute();
                }
                else if (event.isControlDown() && event.getCode() == KeyCode.T) {
                    // I'm sorry, this is very experimental and POC and ugly
                    if (!api.getClientApi().isConnected()) {
                        api.getGuiHandler().showWarningMessage("Not connected to client!");
                        return;
                    }
                    Dialog<Pair<String, String>> dialog = new Dialog<>();
                    dialog.setTitle("Crafting (suuuper test version)");
                    dialog.setHeaderText(null);
                    dialog.setWidth(500);
                    ButtonType loginButtonType = new ButtonType("Craft", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

                    GridPane grid = new GridPane();
                    grid.setMinWidth(500);
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(10, 10, 10, 10));

                    ChoiceBox<Map.Entry<String, Pair<String, Integer>>> tokens = new ChoiceBox<>();
                    tokens.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(Map.Entry<String, Pair<String, Integer>> object) {
                            return object != null ? object.getValue().getKey() : null;
                        }

                        @Override
                        public Map.Entry<String, Pair<String, Integer>> fromString(String string) {
                            return tokens.getItems()
                                    .stream()
                                    .filter(stringPairEntry -> stringPairEntry.getValue().getKey().equals(string))
                                    .findFirst()
                                    .orElse(null);
                        }
                    });
                    ChoiceBox<Map.Entry<String, Pair<String, Integer>>> recipes = new ChoiceBox<>();
                    recipes.setDisable(true);
                    recipes.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(Map.Entry<String, Pair<String, Integer>> object) {
                            return object != null ? object.getValue().getKey() : null;
                        }

                        @Override
                        public Map.Entry<String, Pair<String, Integer>> fromString(String string) {
                            return recipes.getItems()
                                    .stream()
                                    .filter(stringPairEntry -> stringPairEntry.getValue().getKey().equals(string))
                                    .findFirst()
                                    .orElse(null);
                        }
                    });
                    tokens.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        recipes.setDisable(newValue == null);
                        if (newValue != null) {
                            recipes.setItems(FXCollections.observableList(new ArrayList<>(api
                                    .getLootModule()
                                    .getRecipes(newValue.getKey())
                                    .entrySet())));
                        }
                        else {
                            recipes.getItems().clear();
                        }
                    });
                    tokens.setItems(FXCollections.observableList(new ArrayList<>(api
                            .getLootModule()
                            .getEventTokens()
                            .entrySet())));

                    grid.add(new Label("Event token:"), 0, 0);
                    grid.add(tokens, 1, 0);
                    grid.add(new Label("Recipe:"), 0, 1);
                    grid.add(recipes, 1, 1);

                    Node craftButton = dialog.getDialogPane().lookupButton(loginButtonType);
                    craftButton.setDisable(true);

                    recipes.getSelectionModel().selectedItemProperty()
                            .addListener((observable, oldValue, newValue) -> craftButton.setDisable(newValue == null));

                    dialog.getDialogPane().setContent(grid);

                    Platform.runLater(tokens::requestFocus);

                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == loginButtonType) {
                            return new Pair<>(tokens.getSelectionModel()
                                    .getSelectedItem()
                                    .getKey(), recipes.getSelectionModel().getSelectedItem().getKey());
                        }
                        return null;
                    });

                    Optional<Pair<String, String>> result = dialog.showAndWait();

                    result.ifPresent(tokenRecipePair -> {
                        Map.Entry<String, Pair<String, Integer>> token =
                                tokens.getSelectionModel().selectedItemProperty().get();
                        Map.Entry<String, Pair<String, Integer>> recipe =
                                recipes.getSelectionModel().selectedItemProperty().get();
                        int repeat = token.getValue().getValue() / recipe.getValue().getValue();
                        if (repeat == 0) {
                            api.getGuiHandler().showWarningMessage("You don't have enough tokens!");
                        }
                        else {
                            api.getGuiHandler()
                                    .showInfoMessage("Trying to craft the recipe " + repeat + " times");
                            api.getLootModule().craftRecipe(recipe.getKey(), token.getKey(), repeat);
                        }
                    });
                }
                else if (event.isControlDown() && event.getCode() == KeyCode.Y) {
                    Dialog<Boolean> dialog = new Dialog<>();
                    dialog.setTitle("Banning (for lagging client)");
                    dialog.setHeaderText(null);
                    dialog.setWidth(500);
                    ButtonType ban = new ButtonType("Ban", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(ban, ButtonType.CANCEL);
                    StackPane sp = new StackPane();
                    TextField search = new TextField();
                    AutoCompletionBinding<String> autoCompletion =
                            TextFields.bindAutoCompletion(search, (AutoCompletionBinding.ISuggestionRequest param) -> {
                                if (param.getUserText().isEmpty()) {
                                    return new ArrayList<>();
                                }
                                return FuzzySearch
                                        .extractSorted(param.getUserText(), api.getChampions()
                                                .getChampions(), Champion::getName, 3)
                                        .stream()
                                        .map(championBoundExtractedResult -> championBoundExtractedResult.getReferent()
                                                .getName())
                                        .collect(Collectors.toList());
                            });
                    sp.getChildren().add(search);
                    autoCompletion.prefWidthProperty().bind(search.widthProperty());
                    dialog.getDialogPane().setContent(sp);
                    dialog.setResultConverter(dialogButton -> dialogButton == ban);
                    boolean selected = dialog.showAndWait().orElse(false);
                    if (selected) {
                        Champion champion = api.getChampions().getByName(search.getText());
                        if (champion == null) {
                            api.getGuiHandler().showWarningMessage("Invalid champion name!");
                        }
                        else {
                            api.getChampionSelectionModule().banChampion(champion);
                        }
                    }
                }
            });

            Platform.setImplicitExit(false);

            if (!Arrays.asList(api.programArguments).contains("-minimized")) {
                createScene();
            }

            donateDontAsk = SimplePreferences.getBooleanValue(SimplePreferences.InternalKeys.DONATE_DONT_ASK, false);
            AsyncTask.EXECUTOR_SERVICE.submit(AutoUpdater::checkUpdate);
            if (!SimplePreferences.getBooleanValue(SimplePreferences.InternalKeys.ASKED_ANALYTICS, false)) {
                boolean analytics = Settings.openYesNoDialog(
                        api.getLang().getString("analytics_dialog_title"),
                        api.getLang().getString("analytics_dialog_message")
                );
                AnalyticsUtil.onConsent(analytics);
                SimplePreferences.putBooleanValue(SimplePreferences.InternalKeys.ASKED_ANALYTICS, true);
                SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.ANALYTICS, analytics);
            }
        } catch (Throwable e) {
            log.error("Failed to initialize GUI", e);
            AnalyticsUtil.addCrashReport(e, "Failed to initialize GUI", true);
        }
    }

    private void copyRunePage(RunePage runePage) {
        if (runePage != null) {
            StringSelection selection = new StringSelection(runePage.toSerializedString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        }
        api.getGuiHandler().showInfoMessage(api.getLang().getString("successful_rune_copy"));
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
                if (api.getRuneBook().getRuneBookPage(page.getName()) != null) {
                    api.getGuiHandler().showWarningMessage(String.format(api.getLang()
                            .getString("duplicate_name_msg"), page.getName()));
                    return;
                }
                api.getRuneBook().addRuneBookPage(page);
                api.getGuiHandler().showInfoMessage(api.getLang().getString("successful_rune_copy"));
            }
            else {
                api.getGuiHandler().showWarningMessage(api.getLang()
                        .getString("invalid_runepage"));
            }
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Exception occurred while pasting a rune page", e);
        }
    }

    @Subscribe(GAME_PHASE_EVENT)
    public void onGamePhase(ClientEventListener.ClientEvent<LolGameflowGameflowPhase> event) {
        if (event.getData() == LolGameflowGameflowPhase.ENDOFGAME) {
            AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FxUtils.doOnFxThread(() -> {
                    if (api.getChampionSelectionModule().wasLastGameGood()) {
                        showDonateDialog();
                    }
                });
            });
        }
    }

    private void showDonateDialog() {
        if (donateDontAsk) {
            return;
        }
        donateDontAsk = true;
        ButtonType donate = new ButtonType(api.getLang().getString("donate_button"));
        ButtonType later = new ButtonType(api.getLang().getString("later_button"));
        ButtonType never = new ButtonType(api.getLang().getString("never_ask_again_button"));
        ButtonType result = Settings.openDialog(
                api.getLang().getString("donate_dialog_title"),
                api.getLang().getString("donate_dialog_message"),
                donate,
                later,
                never
        );
        if (result == donate) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.paypal.me/stirante"));
                } catch (IOException | URISyntaxException e) {
                    log.error("Exception occurred while navigating to donate page", e);
                    AnalyticsUtil.addCrashReport(e, "Exception occurred while navigating to donate page", false);
                }
            }
        }
        else if (result == never) {
            SimplePreferences.putBooleanValue(SimplePreferences.InternalKeys.DONATE_DONT_ASK, true);
        }
    }

    @Subscribe(value = Runes.RUNE_PAGES_EVENT, priority = EventPriority.LOWEST, eventExecutor = UiEventExecutor.class)
    public static void onRunePagesChange() {
        instance.updateRunes();
    }

    public void updateRunes() {
        if (home == null || runebook == null) {
            return;
        }
        new AsyncTask<Void, Void, List<RunePage>>() {
            @Override
            public List<RunePage> doInBackground(Void[] params) {
                if (!api.getClientApi().isConnected()) {
                    return null;
                }
                try {
                    return api.getRunesModule().getRunePages();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(List<RunePage> result) {
                // During request, destroyScene might have happened, so we need to check it again
                if (home == null || runebook == null) {
                    return;
                }
                home.localRunes.clear();

                // This bit of code is added, because clear causes change in filtered list, which
                // causes IndexOutOfBoundsException in ListView
                // At the end, we set the same list again to ListView
                ObservableList<ChampionBuild> oldList = runebook.localRunesList.getItems();
                runebook.localRunesList.setItems(null);

                runebook.localRunes.clear();
                String title;
                List<ChampionBuild> runeBookValues;
                if (result == null || !api.getClientApi().isConnected()) {
                    title = api.getLang().getString("local_runes_no_connection");
                    runeBookValues = api.getRuneBook().getRuneBookValues();
                }
                else {
                    title = String.format(
                            api.getLang().getString("local_runes"),
                            result.size(),
                            api.getRunesModule().getOwnedPageCount());
                    // Split list into runepages, that are both in runebook and in client and those, that are only in runebook
                    Map<Boolean, List<ChampionBuild>> results =
                            new ArrayList<>(api.getRuneBook().getRuneBookValues())
                                    .stream()
                                    .collect(Collectors.partitioningBy(runePage -> result
                                            .stream()
                                            .noneMatch(runePage1 -> runePage1.equals(runePage.getRunePage()))));
                    for (RunePage page : result.stream()
                            .filter(p -> results.get(false)
                                    .stream()
                                    .anyMatch(runePage -> runePage.getName().equals(p.getName())))
                            .collect(Collectors.toCollection(ArrayList::new))) {
                        page.setSynced(true);
                    }
                    runeBookValues = result.stream()
                            .map(runePage -> ChampionBuild.builder(runePage).create())
                            .collect(Collectors.toCollection(ArrayList::new));
                    runeBookValues.addAll(results.get(true));
                }
                home.localRunesTitle.setText(title);
                runebook.localRunesTitle.setText(title);
                home.localRunes.addAll(runeBookValues);
                runebook.localRunes.addAll(runeBookValues);
                runebook.localRunesList.setItems(oldList);
            }
        }.execute();
    }
}
