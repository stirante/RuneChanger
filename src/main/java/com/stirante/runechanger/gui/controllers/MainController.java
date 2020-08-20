package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Content;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.util.LangHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MainController.class);
    private final Stage stage;

    public TextField search;
    public ImageView back;
//    public Button report;
    public Pane fullContentPane;
    public Pane contentPane;
    public Pane container;

    private Consumer<Champion> searchHandler;

    private double xOffset;
    private double yOffset;

    private Content content;

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
//        report.getTooltip().setShowDelay(Duration.ZERO);
    }

    public void setFullContent(Content content) {
        if (this.content != null) {
            this.content.onDetach();
        }
        this.content = content;
        fullContentPane.getChildren().clear();
        fullContentPane.getChildren().add(content.getNode());
        fullContentPane.setVisible(true);
        contentPane.setVisible(false);
//        report.setVisible(false);
        back.setVisible(true);
    }

    public void setContent(Content content) {
        if (this.content != null) {
            this.content.onDetach();
        }
        this.content = content;
        contentPane.getChildren().clear();
        contentPane.getChildren().add(content.getNode());
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
//        report.setVisible(true);
        back.setVisible(false);
    }

    @FXML
    public void onSettings(MouseEvent event) {
        setFullContent(new SettingsController(stage));
    }

    @FXML
    public void onHandlePress(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    public void onHandleDrag(MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    public void onClose(MouseEvent event) {
        System.exit(0);
    }

    @FXML
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

    @FXML
    public void onBack(MouseEvent mouseEvent) {
        if (content != null) {
            content.onDetach();
            content = null;
        }
        fullContentPane.getChildren().clear();
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
//        report.setVisible(true);
        back.setVisible(false);
    }
}
