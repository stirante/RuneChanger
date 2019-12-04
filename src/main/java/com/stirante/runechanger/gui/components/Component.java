package com.stirante.runechanger.gui.components;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Text;

import java.util.ArrayList;

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

    /**
     *
     * @param g Graphics context
     * @param text Text to fill
     * @param x center x
     * @param y center y
     * @param spacing spacing between letters (kerning)
     */
    protected void fillNiceCenteredText(GraphicsContext g, String text, double x, double y, double spacing) {
        ArrayList<Double> widths = new ArrayList<>(text.length());
        Text t = new Text("");
        t.setFont(g.getFont());
        t.setWrappingWidth(0);
        t.setLineSpacing(0);
        t.prefWidth(-1);
        t.prefHeight(-1);
        double width = 0;
        double height = -1;
        for (int i = 0; i < text.length(); i++) {
            t.setText(String.valueOf(text.charAt(i)));
            t.setWrappingWidth(0);
            t.setLineSpacing(0);
            t.prefWidth(-1);
            if (i != 0) {
                width += spacing;
            }
            width += t.getLayoutBounds().getWidth();
            if (height == -1) {
                height = t.getLayoutBounds().getHeight();
            }
            widths.add(t.getLayoutBounds().getWidth());
        }
        x = x - (width / 2);
        y = y + (height / 4);
        for (int i = 0; i < text.length(); i++) {
            g.fillText(String.valueOf(text.charAt(i)), x, y);
            if (i != text.length() - 1) {
                x += spacing;
            }
            x += widths.get(i);
        }
    }

}
