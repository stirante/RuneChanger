package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Content;
import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.util.*;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SettingsController implements Content {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    private final static int TRANSITION_DURATION = 200;
    private final static double BASE_MODIFIER = 2;
    private final Stage stage;
    public Pane container;
    public VBox wrapper;
    public ScrollPane scroll;

    public SettingsController(Stage stage) {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Settings.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fixScroll();
        loadPreferences();
    }

    private void tryRestart() {
        boolean restart = Settings.openYesNoDialog(LangHelper.getLang()
                .getString("restart_necessary"), LangHelper.getLang()
                .getString("restart_necessary_description"));
        if (restart) {
            restartProgram();
        }
    }

    private void loadPreferences() {
        setupPreference(SimplePreferences.SettingsKeys.QUICK_REPLIES, false, "quick_replies", "quick_replies_message");
        setupPreference(SimplePreferences.SettingsKeys.AUTO_ACCEPT, false, "auto_queue", "auto_queue_message");
        setupPreference(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false, "restart_on_dodge", "restart_on_dodge_message");
        setupPreference(SimplePreferences.SettingsKeys.ANTI_AWAY, false, "no_away", "no_away_message");
        setupPreference(SimplePreferences.SettingsKeys.AUTO_SYNC, false, "auto_sync_pages", "auto_sync_pages_message", selected -> tryRestart());
        setupPreference(SimplePreferences.SettingsKeys.SMART_DISENCHANT, false, "smart_disenchant", "smart_disenchant_message");
        setupPreference(SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, true, "champion_suggestions", "champion_suggestions_message");
        setupPreference(SimplePreferences.SettingsKeys.AUTO_UPDATE, true, "autoupdate_state", "autoupdate_message");
        setupPreference(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, false, "autoupdate_experimental", "autoupdate_experimental_message", selected -> {
            AutoUpdater.resetCache();
            AutoUpdater.checkUpdate();
        });
        setupPreference(SimplePreferences.SettingsKeys.FORCE_ENGLISH, false, "force_english", "force_english_message", selected -> tryRestart());
        setupPreference(SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, false, "always_on_top", "always_on_top_message", stage::setAlwaysOnTop);
        setupPreference(SimplePreferences.SettingsKeys.ANALYTICS, true, "enable_analytics", "enable_analytics_message", AnalyticsUtil::onConsent);
        setupPreference(SimplePreferences.SettingsKeys.ENABLE_ANIMATIONS, true, "enable_animations", "enable_animations_message");
        setupPreference(SimplePreferences.SettingsKeys.RUN_AS_ADMIN, false, "run_as_admin", "run_as_admin_message", selected -> tryRestart());

        wrapper.getChildren().add(new SettingsItemController(
                AutoStartUtils.isAutoStartEnabled(),
                AutoStartUtils::setAutoStart,
                LangHelper.getLang().getString("autostart"),
                LangHelper.getLang().getString("autostart_message")
        ).getRoot());

//        wrapper.getChildren().add(new SettingsEditItemController(
//                FxUtils.prop("Initial text", (observable, oldValue, newValue) -> {
//                    System.out.println("Text changed to " + newValue);
//                }),
//                null,
//                "Test text value",
//                "Without checkbox"
//        ).getRoot());
//        wrapper.getChildren().add(new SettingsEditItemController(
//                FxUtils.prop("Initial text", FxUtils.delayedChangedListener(
//                        (observable, oldValue, newValue) -> System.out.println("Text changed to " + newValue)
//                )),
//                FxUtils.prop(false, (observable, oldValue, newValue) -> System.out.println("Checked: " + newValue)),
//                "Test text value",
//                "With checkbox"
//        ).getRoot());
    }

    private void setupPreference(String key, boolean defaultValue, String titleKey, String descKey) {
        setupPreference(key, defaultValue, titleKey, descKey, null);
    }

    private void setupPreference(String key, boolean defaultValue, String titleKey, String descKey, Consumer<Boolean> additionalAction) {
        if (!SimplePreferences.containsKey(key)) {
            SimplePreferences.putBooleanValue(key, defaultValue);
        }
        boolean val = SimplePreferences.getBooleanValue(key, defaultValue);
        wrapper.getChildren().add(new SettingsItemController(val, selected -> {
            SimplePreferences.putBooleanValue(key, selected);
            if (additionalAction != null) {
                additionalAction.accept(selected);
            }
            SimplePreferences.save();
        }, LangHelper.getLang().getString(titleKey), LangHelper.getLang().getString(descKey)).getRoot());
    }

    private void restartProgram() {
        SimplePreferences.save();
        try {
            Runtime.getRuntime().exec("wscript silent.vbs open.bat");
        } catch (Exception ex) {
            log.error("Exception occurred while executing a restart command", ex);
            AnalyticsUtil.addCrashReport(ex, "Exception occurred while executing a restart command", false);
        }
        System.exit(0);
    }

    /*
     * From https://gist.github.com/Col-E/7d31b6b8684669cf1997831454681b85
     */

    private void fixScroll() {
        wrapper.setOnScroll(new EventHandler<>() {
            private SmoothishTransition transition;

            @Override
            public void handle(ScrollEvent event) {
                double deltaY = BASE_MODIFIER * event.getDeltaY();
                double width = scroll.getContent().getBoundsInLocal().getWidth();
                double vvalue = scroll.getVvalue();
                Interpolator interp = Interpolator.LINEAR;
                transition = new SmoothishTransition(transition, deltaY) {
                    @Override
                    protected void interpolate(double frac) {
                        double x = interp.interpolate(vvalue, vvalue + -deltaY * getMod() / width, frac);
                        scroll.setVvalue(x);
                    }
                };
                transition.play();
            }
        });
    }

    /**
     * @param t Transition to check.
     * @return {@code true} if transition is playing.
     */
    private static boolean playing(Transition t) {
        return t.getStatus() == Animation.Status.RUNNING;
    }

    /**
     * @param d1 Value 1
     * @param d2 Value 2.
     * @return {@code true} if value signs are matching.
     */
    private static boolean sameSign(double d1, double d2) {
        return (d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0);
    }

    @Override
    public void onDetach() {

    }

    @Override
    public Node getNode() {
        return container;
    }

    /**
     * Transition with varying speed based on previously existing transitions.
     *
     * @author Matt
     */
    abstract static class SmoothishTransition extends Transition {
        private final double mod;
        private final double delta;

        public SmoothishTransition(SmoothishTransition old, double delta) {
            setCycleDuration(Duration.millis(TRANSITION_DURATION));
            setCycleCount(0);
            // if the last transition was moving inthe same direction, and is still playing
            // then increment the modifer. This will boost the distance, thus looking faster
            // and seemingly consecutive.
            if (old != null && sameSign(delta, old.delta) && playing(old)) {
                mod = old.getMod() + 1;
            }
            else {
                mod = 1;
            }
            this.delta = delta;
        }

        public double getMod() {
            return mod;
        }

        @Override
        public void play() {
            super.play();
            // Even with a linear interpolation, startup is visibly slower than the middle.
            // So skip a small bit of the animation to keep up with the speed of prior
            // animation. The value of 10 works and isn't noticeable unless you really pay
            // close attention. This works best on linear but also is decent for others.
            if (getMod() > 1) {
                jumpTo(getCycleDuration().divide(10));
            }
        }
    }

}
