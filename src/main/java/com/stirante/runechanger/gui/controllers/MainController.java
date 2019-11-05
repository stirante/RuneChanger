package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.util.LangHelper;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {
    private final Stage stage;

    public TextField search;
    public ImageView back;
    public Pane sidebar;
    public Button report;
    public Pane fullContentPane;
    public Pane contentPane;
    public Pane container;

    public Button settings;
//    public Button gameSettings;
//    public Button otherSettings;
//    public Button loot;
    public Button[] sidebarButtons;

    private Consumer<Champion> searchHandler;

    private double xOffset;
    private double yOffset;

    public MainController(Stage stage) {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AutoCompletionBinding<String> autoCompletion =
                TextFields.bindAutoCompletion(search, (AutoCompletionBinding.ISuggestionRequest param) -> {
                    if (param.getUserText().isEmpty()) {
                        return new ArrayList<>();
                    }
                    return FuzzySearch
                            .extractSorted(param.getUserText(), Champion.values(), Champion::getName, 3)
                            .stream()
                            .map(championBoundExtractedResult -> championBoundExtractedResult.getReferent().getName())
                            .collect(Collectors.toList());
                });
        autoCompletion.setOnAutoCompleted(this::onSearch);
        autoCompletion.prefWidthProperty().bind(search.widthProperty());
        back.setVisible(false);
        sidebarButtons = new Button[]{settings/*, gameSettings, otherSettings, loot*/};
    }

    public void setFullContent(Node node) {
        fullContentPane.getChildren().clear();
        fullContentPane.getChildren().add(node);
        fullContentPane.setVisible(true);
        contentPane.setVisible(false);
        sidebar.setVisible(false);
        report.setVisible(false);
        back.setVisible(true);
    }

    public void setContent(Node node) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(node);
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
        sidebar.setVisible(true);
        report.setVisible(true);
        back.setVisible(false);
    }

    public void onBugReport(ActionEvent actionEvent) {

    }

    public void onTabSelect(ActionEvent actionEvent) {
        if (actionEvent.getTarget() == settings) {
            setFullContent(new SettingsController(stage).container);
        }
//        Button clicked = (Button) actionEvent.getTarget();
//        KeyValue[] startValues = new KeyValue[sidebarButtons.length];
//        KeyValue[] endValues = new KeyValue[sidebarButtons.length];
//        int i = 0;
//        for (Button button : sidebarButtons) {
//            if (button == clicked) continue;
//            startValues[i] = new KeyValue(button.translateXProperty(), button.translateXProperty().doubleValue(), Interpolator.EASE_OUT);
//            endValues[i] = new KeyValue(button.translateXProperty(), 0, Interpolator.EASE_OUT);
//            i++;
//        }
//        startValues[i] = new KeyValue(clicked.translateXProperty(), clicked.translateXProperty().doubleValue(), Interpolator.EASE_OUT);
//        endValues[i] = new KeyValue(clicked.translateXProperty(), 20, Interpolator.EASE_OUT);
//        KeyFrame start = new KeyFrame(Duration.ZERO, startValues);
//        KeyFrame end = new KeyFrame(Duration.millis(100), endValues);
//        Timeline timeline = new Timeline(start, end);
//        timeline.play();
    }

    public void onHandlePress(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void onHandleDrag(MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    public void onClose(MouseEvent event) {
        System.exit(0);
    }

    public void onMinimize(MouseEvent event) {
        stage.hide();
    }

    public void setOnChampionSearch(Consumer<Champion> handler) {
        this.searchHandler = handler;
    }

    public void onSearch(AutoCompletionBinding.AutoCompletionEvent event) {
        if (searchHandler != null) {
            searchHandler.accept(Champion.getByName((String) event.getCompletion()));
        }
        search.setText("");
        search.getParent().requestFocus();
    }

    public void onBack(MouseEvent mouseEvent) {
        fullContentPane.getChildren().clear();
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
        sidebar.setVisible(true);
        report.setVisible(true);
        back.setVisible(false);
    }
}
