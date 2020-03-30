package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.util.*;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SettingsController {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    private final static int TRANSITION_DURATION = 200;
    private final static double BASE_MODIFIER = 2;
    private final Stage stage;
    public CheckBox antiAway;
    public CheckBox autoAccept;
    public CheckBox quickReplies;
    public CheckBox autoUpdate;
    public CheckBox alwaysOnTop;
    public CheckBox autoStart;
    public CheckBox forceEnglish;
    public CheckBox experimental;
    public CheckBox autoSync;
    public CheckBox smartDisenchant;
    public CheckBox championSuggestions;
    public CheckBox enableAnalytics;
    public CheckBox enableAnimations;
    public CheckBox restartOnDodge;
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

    @FXML
    void handleCheckboxPressed(ActionEvent e) {
        CheckBox target = (CheckBox) e.getTarget();
        if (target == autoStart) {
            AutoStartUtils.setAutoStart(target.isSelected());
        }
        else if (target == autoAccept) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.AUTO_ACCEPT, target.isSelected());
        }
        else if (target == antiAway) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.ANTI_AWAY, target.isSelected());
        }
        else if (target == quickReplies) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.QUICK_REPLIES, target.isSelected());
        }
        else if (target == autoUpdate) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.AUTO_UPDATE, target.isSelected());
        }
        else if (target == smartDisenchant) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.SMART_DISENCHANT, target.isSelected());
        }
        else if (target == enableAnalytics) {
            AnalyticsUtil.onConsent(target.isSelected());
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.ANALYTICS, target.isSelected());
        }
        else if (target == experimental) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, target.isSelected());
            AutoUpdater.resetCache();
            AutoUpdater.checkUpdate();
        }
        else if (target == championSuggestions) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, target.isSelected());
        }
        else if (target == autoSync) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.AUTO_SYNC, target.isSelected());
            tryRestart();
        }
        else if (target == alwaysOnTop) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, target.isSelected());
            stage.setAlwaysOnTop(target.isSelected());
        }
        else if (target == forceEnglish) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.FORCE_ENGLISH, target.isSelected());
            tryRestart();
        }
        else if (target == enableAnimations) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.ENABLE_ANIMATIONS, target.isSelected());
        }
        else if (target == restartOnDodge) {
            SimplePreferences.putBooleanValue(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, target.isSelected());
        }
        SimplePreferences.save();
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
        setupPreference(SimplePreferences.SettingsKeys.QUICK_REPLIES, false, quickReplies);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_ACCEPT, false, autoAccept);
        setupPreference(SimplePreferences.SettingsKeys.ANTI_AWAY, false, antiAway);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_UPDATE, true, autoUpdate);
        setupPreference(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, false, experimental);
        setupPreference(SimplePreferences.SettingsKeys.FORCE_ENGLISH, false, forceEnglish);
        setupPreference(SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, false, alwaysOnTop);
        setupPreference(SimplePreferences.SettingsKeys.AUTO_SYNC, false, autoSync);
        setupPreference(SimplePreferences.SettingsKeys.SMART_DISENCHANT, false, smartDisenchant);
        setupPreference(SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, true, championSuggestions);
        setupPreference(SimplePreferences.SettingsKeys.ANALYTICS, true, enableAnalytics);
        setupPreference(SimplePreferences.SettingsKeys.ENABLE_ANIMATIONS, true, enableAnimations);
        setupPreference(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false, restartOnDodge);

        if (AutoStartUtils.isAutoStartEnabled()) {
            autoStart.setSelected(true);
        }
    }

    private void setupPreference(String key, boolean defaultValue, CheckBox checkbox) {
        if (!SimplePreferences.containsKey(key)) {
            SimplePreferences.putBooleanValue(key, defaultValue);
        }
        if (SimplePreferences.getBooleanValue(key, defaultValue)) {
            Platform.runLater(() -> checkbox.setSelected(true));
        }
    }

    private void restartProgram() {
        SimplePreferences.save();
        try {
            Runtime.getRuntime().exec("wscript silent.vbs open.bat");
        } catch (Exception ex) {
            log.error("Exception occurred while executing a restart command", ex);
            if (Countly.isInitialized()) {
                Countly.session().addCrashReport(ex, false);
            }
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
     * @return {@code true} if values signes are matching.
     */
    private static boolean sameSign(double d1, double d2) {
        return (d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0);
    }

    /**
     * Transition with varying speed based on previously existing transitions.
     *
     * @author Matt
     */
    abstract class SmoothishTransition extends Transition {
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
