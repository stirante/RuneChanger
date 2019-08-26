package com.stirante.RuneChanger.gui.components;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

public abstract class Component extends Pane {

    private final Canvas canvas;

    public Component() {
        canvas = new Canvas();
        getChildren().add(canvas);

        //Invalidate currently rendered element on change
        widthProperty().addListener(this::invalidated);
        heightProperty().addListener(this::invalidated);
        disabledProperty().addListener(this::invalidated);

        //Bind canvas size to pane size
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
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
