package com.stirante.runechanger.gui.components;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public abstract class Component extends Pane implements Initializable {

    @FXML
    private Canvas canvas;
    private Pane view;

    public Component() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Component.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            view = fxmlLoader.load();

        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        //Invalidate currently rendered element on change
        widthProperty().addListener(this::invalidated);
        heightProperty().addListener(this::invalidated);
        disabledProperty().addListener(this::invalidated);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Bind sizes
        canvas.widthProperty().bind(view.widthProperty());
        canvas.heightProperty().bind(view.heightProperty());
        view.prefHeightProperty().bind(widthProperty());
        view.prefHeightProperty().bind(heightProperty());
    }

    public void invalidate() {
        if (Platform.isFxApplicationThread()) {
            render(canvas.getGraphicsContext2D());
        }
        else {
            Platform.runLater(() -> render(canvas.getGraphicsContext2D()));
        }
    }

    public abstract void render(GraphicsContext g);

    protected void invalidated(Observable observable) {
        invalidate();
    }
}
