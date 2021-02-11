package com.stirante.runechanger.util;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FxUtils {

    /**
     * Executes runnable on FX thread or runs it, if it's already FX thread
     *
     * @param runnable runnable to execute
     */
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
        if (LangHelper.isTextRTL()) {
            Text t = new Text(text);
            t.setFont(font);
            t.setWrappingWidth(0);
            t.setLineSpacing(0);
            t.prefWidth(-1);
            t.prefHeight(-1);
            return new Pair<>(t.getLayoutBounds().getWidth(), t.getLayoutBounds().getHeight());
        }
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
        if (LangHelper.isTextRTL()) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(text, x, y);
            return;
        }
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

    /**
     * Creates ChangeListener, that fires original only after no change has been made for specified time
     *
     * @param onChange ChangeListener to fire
     * @param delay    specified time in milliseconds, after which the listener will be fired
     * @param <T>      type
     * @return ChangeListener, that will be delayed
     */
    public static <T> ChangeListener<T> delayedChangedListener(CancellableChangeListener<T> onChange, long delay) {
        return new ChangeListener<T>() {
            private Timer timer;
            private T oValue;
            private T nValue;
            private ObservableValue<? extends T> observableValue;
            private boolean canceling = false;

            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                if (!canceling) {
                    nValue = newValue;
                    if (oValue == null) {
                        oValue = oldValue;
                        observableValue = observable;
                    }
                    if (timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            doOnFxThread(() -> {
                                if (!onChange.changed(observableValue, oValue, nValue)) {
                                    canceling = true;
                                    Platform.runLater(() -> {
                                        if (!(observableValue instanceof WritableValue)) {
                                            throw new IllegalStateException("Can't cancel change in read-only value!");
                                        }
                                        //noinspection unchecked
                                        ((WritableValue<T>) observableValue).setValue(oValue);
                                        canceling = false;
                                    });
                                }
                                else {
                                    observableValue = null;
                                    oValue = null;
                                }
                            });
                        }
                    }, delay);
                }
            }

        };
    }

    /**
     * Creates ChangeListener, that fires original only after no change has been made for one second
     *
     * @param onChange ChangeListener to fire
     * @param <T>      type
     * @return ChangeListener, that will be delayed
     */
    public static <T> ChangeListener<T> delayedChangedListener(CancellableChangeListener<T> onChange) {
        return delayedChangedListener(onChange, 1000);
    }


    public static <T> Property<T> prop(T initialValue, ChangeListener<T> onChange) {
        SimpleObjectProperty<T> prop = new SimpleObjectProperty<>(initialValue);
        prop.addListener(onChange);
        return prop;
    }

    @FunctionalInterface
    public interface CancellableChangeListener<T> {

        /**
         * This method needs to be provided by an implementation of
         * {@code ChangeListener}. It is called if the value of an
         * {@link ObservableValue} changes.
         * <p>
         * In general, it is considered bad practice to modify the observed value in
         * this method.
         *
         * @param observable The {@code ObservableValue} which value changed
         * @param oldValue   The old value
         * @param newValue   The new value
         */
        boolean changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
    }

}
