package com.stirante.runechanger.util;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class FxUtils {

    public static void doOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(runnable);
        }
    }

    private static Pair<Double, List<Double>> getTextSizes(Font font, String text) {
        ArrayList<Double> widths = new ArrayList<>(text.length());
        Text t = new Text("");
        t.setFont(font);
        t.setWrappingWidth(0);
        t.setLineSpacing(0);
        t.prefWidth(-1);
        t.prefHeight(-1);
        double height = -1;
        for (int i = 0; i < text.length(); i++) {
            t.setText(String.valueOf(text.charAt(i)));
            t.setWrappingWidth(0);
            t.setLineSpacing(0);
            t.prefWidth(-1);
            if (height == -1) {
                height = t.getLayoutBounds().getHeight();
            }
            widths.add(t.getLayoutBounds().getWidth());
        }
        return new Pair<>(height, widths);
    }

    private static Pair<Double, Double> measureTextSize(Pair<Double, List<Double>> textSizes, String text, double spacing) {
        double width = 0;
        for (int i = 0; i < text.length(); i++) {
            if (i != 0) {
                width += spacing;
            }
            width += textSizes.getValue().get(i);
        }
        return new Pair<>(width, textSizes.getKey());
    }

    /**
     * Measures text size
     *
     * @param font    Font
     * @param text    Text to fill
     * @param spacing spacing between letters (kerning)
     * @return Pair of doubles, where key is width and value is height
     */
    public static Pair<Double, Double> measureTextSize(Font font, String text, double spacing) {
        Pair<Double, List<Double>> textSizes = getTextSizes(font, text);
        return measureTextSize(textSizes, text, spacing);
    }

    /**
     * @param g       Graphics context
     * @param text    Text to fill
     * @param x       center x
     * @param y       center y
     * @param spacing spacing between letters (kerning)
     */
    public static void fillNiceCenteredText(GraphicsContext g, String text, double x, double y, double spacing) {
        Pair<Double, List<Double>> textSizes = getTextSizes(g.getFont(), text);
        List<Double> widths = textSizes.getValue();
        Pair<Double, Double> size = measureTextSize(textSizes, text, spacing);
        double width = size.getKey();
        double height = size.getValue();

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
