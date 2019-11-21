package com.stirante.runechanger.gui.components;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public abstract class Component extends Canvas {

    public Component() {
        initialize();
    }

    public void initialize() {
        //Invalidate currently rendered element on change
        widthProperty().addListener(this::invalidated);
        heightProperty().addListener(this::invalidated);
        disabledProperty().addListener(this::invalidated);
    }

    public void invalidate() {
        if (Platform.isFxApplicationThread()) {
            render(getGraphicsContext2D());
        }
        else {
            Platform.runLater(() -> render(getGraphicsContext2D()));
        }
    }

    public abstract void render(GraphicsContext g);

    protected void invalidated(Observable observable) {
        invalidate();
    }

}
