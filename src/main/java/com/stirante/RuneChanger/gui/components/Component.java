package com.stirante.RuneChanger.gui.components;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.IOException;

public abstract class Component extends Pane {

    private Canvas canvas;

    public Component() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Component.fxml"));
        fxmlLoader.setController(this);
        Pane view;
        try {
            view = fxmlLoader.load();
            getChildren().add(view);

        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        //Invalidate currently rendered element on change
        widthProperty().addListener(this::invalidated);
        heightProperty().addListener(this::invalidated);
        disabledProperty().addListener(this::invalidated);

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
