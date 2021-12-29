package com.stirante.runechanger.gui.components;

import com.stirante.runechanger.utils.FxUtils;
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
        FxUtils.doOnFxThread(() -> render(getGraphicsContext2D()));
    }

    public abstract void render(GraphicsContext g);

    protected void invalidated(Observable observable) {
        invalidate();
    }

}
