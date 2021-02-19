package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.gui.Content;
import com.stirante.runechanger.gui.Settings;
import com.stirante.runechanger.model.app.SettingsConfiguration;
import com.stirante.runechanger.sourcestore.Source;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.util.*;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SettingsController implements Content {
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    private final static int TRANSITION_DURATION = 200;
    private final static double BASE_MODIFIER = 2;

    private static final String CLIENT_CATEGORY = "client";
    private static final String APP_CATEGORY = "app";
    private static final String MESSAGES_CATEGORY = "messages";
    private static final String SOURCES_CATEGORY = "sources";

    private final List<SettingsCategoryController> categoryControllers = new ArrayList<>();
    private final Map<String, List<Node>> categoryContents = new HashMap<>();
    private final Stage stage;

    public Pane container;
    public HBox categoryBar;
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
        Platform.runLater(() -> {
            boolean restart = Settings.openYesNoDialog(LangHelper.getLang()
                    .getString("restart_necessary"), LangHelper.getLang()
                    .getString("restart_necessary_description"));
            if (restart) {
                restartProgram();
            }
        });
    }

    private void loadPreferences() {
        // Setup categories
        setupCategory(CLIENT_CATEGORY, "client_category");
        setupCategory(MESSAGES_CATEGORY, "messages_category");
        setupCategory(SOURCES_CATEGORY, "sources_category");
        setupCategory(APP_CATEGORY, "app_category");

        // Setup preferences
        setupSimplePreference(MESSAGES_CATEGORY, SimplePreferences.SettingsKeys.QUICK_REPLIES, false, "quick_replies", "quick_replies_message");
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.AUTO_MESSAGE, false,
                SimplePreferences.SettingsKeys.AUTO_MESSAGE_TEXT, "",
                "auto_message", "auto_message_desc");
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.CUSTOM_MESSAGE, false,
                SimplePreferences.SettingsKeys.CUSTOM_MESSAGE_TEXT, "",
                "custom_message", "custom_message_desc");
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.ADC_MESSAGE, "bot",
                "custom_adc_message", null);
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.SUPP_MESSAGE, "supp",
                "custom_supp_message", null);
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.MID_MESSAGE, "mid",
                "custom_mid_message", null);
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.JUNGLE_MESSAGE, "jungle",
                "custom_jungle_message", null);
        setupTextPreference(MESSAGES_CATEGORY,
                SimplePreferences.SettingsKeys.TOP_MESSAGE, "top",
                "custom_top_message", null);

        setupSimplePreference(CLIENT_CATEGORY, SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false, "restart_on_dodge", "restart_on_dodge_message");
        setupSimplePreference(CLIENT_CATEGORY, SimplePreferences.SettingsKeys.ANTI_AWAY, false, "no_away", "no_away_message");
        setupSimplePreference(CLIENT_CATEGORY, SimplePreferences.SettingsKeys.AUTO_SYNC, false, "auto_sync_pages", "auto_sync_pages_message", toTruePredicate(this::tryRestart));
        setupSimplePreference(CLIENT_CATEGORY, SimplePreferences.SettingsKeys.SMART_DISENCHANT, false, "smart_disenchant", "smart_disenchant_message");
        setupSimplePreference(CLIENT_CATEGORY, SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, true, "champion_suggestions", "champion_suggestions_message");

        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.AUTO_UPDATE, true, "autoupdate_state", "autoupdate_message");
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, false, "autoupdate_experimental", "autoupdate_experimental_message", selected -> {
            AutoUpdater.resetCache();
            AutoUpdater.checkUpdate();
            return true;
        });
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.FORCE_ENGLISH, false, "force_english", "force_english_message", toTruePredicate(this::tryRestart));
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.ALWAYS_ON_TOP, false, "always_on_top", "always_on_top_message", toTruePredicate(stage::setAlwaysOnTop));
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.ANALYTICS, true, "enable_analytics", "enable_analytics_message", toTruePredicate(AnalyticsUtil::onConsent));
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.ENABLE_ANIMATIONS, true, "enable_animations", "enable_animations_message");
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.RUN_AS_ADMIN, false, "run_as_admin", "run_as_admin_message", toTruePredicate(this::tryRestart));
        setupSimplePreference(APP_CATEGORY, SimplePreferences.SettingsKeys.AUTO_EXIT, false, "exit_rune_changer_with_client", "exit_rune_changer_with_client_message");
        setupPreference(APP_CATEGORY, new SettingsItemController(
                AutoStartUtils.isAutoStartEnabled(),
                toTruePredicate(AutoStartUtils::setAutoStart),
                LangHelper.getLang().getString("autostart"),
                LangHelper.getLang().getString("autostart_message"),
                false
        ).getRoot());

        for (Source source : SourceStore.getSources()) {
            SettingsConfiguration config = new SettingsConfiguration();
            source.setupSettings(config);
            setupSourcePreference(source.getSourceKey(), source.getSourceName(), config.getFields().size() > 0);
            List<SettingsConfiguration.FieldConfiguration<?>> fields = config.getFields();
            for (int i = 0; i < fields.size(); i++) {
                SettingsConfiguration.FieldConfiguration<?> field = fields.get(i);
                if (field.getType() == Boolean.class) {
                    setupSimplePreference(
                            SOURCES_CATEGORY,
                            field.getPrefKey(source.getSourceKey()),
                            field.getDefaultValue() != null && (boolean) field.getDefaultValue(),
                            field.getTitleKey(source.getSourceKey()),
                            field.getDescKey(source.getSourceKey()),
                            s -> {
                                //noinspection unchecked
                                if (field.getValidator() == null || (((Predicate<Boolean>) field.getValidator()).test(s))) {
                                    Platform.runLater(() -> {
                                        SourceStore.updateSourceSettings(source);
                                    });
                                    return true;
                                }
                                else {
                                    return false;
                                }
                            },
                            i < fields.size() - 1
                    );
                }
                else if (field.getType() == String.class) {
                    setupTextPreference(
                            SOURCES_CATEGORY,
                            field.getPrefKey(source.getSourceKey()),
                            field.getDefaultValue() != null ? (String) field.getDefaultValue() : null,
                            field.getTitleKey(source.getSourceKey()),
                            field.getDescKey(source.getSourceKey()),
                            s -> {
                                //noinspection unchecked
                                if (field.getValidator() == null || (((Predicate<String>) field.getValidator()).test(s))) {
                                    Platform.runLater(() -> {
                                        SourceStore.updateSourceSettings(source);
                                    });
                                    return true;
                                }
                                else {
                                    return false;
                                }
                            },
                            i < fields.size() - 1
                    );
                }
                else {
                    throw new IllegalArgumentException("Unexpected setting type " + field.getType().getSimpleName());
                }
            }
        }

        // Finally display first category
        displayCategory(categoryControllers.get(0).getCategoryId());
    }

    private void setupCategory(String id, String titleKey) {
        categoryContents.put(id, new ArrayList<>());
        SettingsCategoryController controller =
                new SettingsCategoryController(id, LangHelper.getLang().getString(titleKey), () -> {
                    displayCategory(id);
                });
        categoryControllers.add(controller);
        categoryBar.getChildren().add(controller.getRoot());
    }

    private void setupSimplePreference(String category, String key, boolean defaultValue, String titleKey, String descKey) {
        setupSimplePreference(category, key, defaultValue, titleKey, descKey, null, false);
    }

    private void setupSimplePreference(String category, String key, boolean defaultValue, String titleKey, String descKey, Predicate<Boolean> onChange) {
        setupSimplePreference(category, key, defaultValue, titleKey, descKey, onChange, false);
    }

    private void setupSimplePreference(String category, String key, boolean defaultValue, String titleKey, String descKey, Predicate<Boolean> onChange, boolean hideSeparator) {
        if (!SimplePreferences.containsKey(key)) {
            SimplePreferences.putBooleanValue(key, defaultValue);
        }
        boolean val = SimplePreferences.getBooleanValue(key, defaultValue);
        setupPreference(category, new SettingsItemController(val, selected -> {
            if (onChange != null && !onChange.test(selected)) {
                return false;
            }
            SimplePreferences.putBooleanValue(key, selected);
            SimplePreferences.save();
            return true;
        }, LangHelper.getLang().getString(titleKey), LangHelper.getLang().getString(descKey), hideSeparator).getRoot());
    }

    private void setupSourcePreference(String key, String title, boolean hideSeparator) {
        if (!SimplePreferences.containsKey(key)) {
            SimplePreferences.putBooleanValue(key, true);
        }
        boolean val = SimplePreferences.getBooleanValue(key, true);
        SettingsItemController controller = new SettingsItemController(val, selected -> {
            SimplePreferences.putBooleanValue(key, selected);
            SimplePreferences.save();
            SourceStore.invalidateCaches();
            return true;
        }, title, LangHelper.getLang().getString("source_msg"), hideSeparator);
        setupPreference(SOURCES_CATEGORY, controller.getRoot());
    }

    private void setupTextPreference(String category, String checkKey, boolean defaultCheck, String textKey, String defaultText, String titleKey, String descKey) {
        setupTextPreference(category, checkKey, defaultCheck, textKey, defaultText, titleKey, descKey, null, false);
    }

    private void setupTextPreference(String category, String checkKey, boolean defaultCheck, String textKey, String defaultText, String titleKey, String descKey, Predicate<String> onChange, boolean hideSeparator) {
        boolean check = SimplePreferences.getBooleanValue(checkKey, defaultCheck);
        String text = SimplePreferences.getStringValue(textKey, defaultText);
        setupPreference(category, new SettingsEditItemController(
                text,
                FxUtils.prop(check, (observable, oldValue, newValue) -> SimplePreferences.putBooleanValue(checkKey, newValue)),
                LangHelper.getLang().getString(titleKey),
                descKey != null ? LangHelper.getLang().getString(descKey) : null,
                s -> {
                    if (onChange != null && !onChange.test(s)) {
                        return false;
                    }
                    SimplePreferences.putStringValue(textKey, s);
                    SimplePreferences.save();
                    return true;
                }, hideSeparator
        ).getRoot());
    }

    private void setupTextPreference(String category, String textKey, String defaultText, String titleKey, String descKey) {
        setupTextPreference(category, textKey, defaultText, titleKey, descKey, null, false);
    }

    private void setupTextPreference(String category, String textKey, String defaultText, String titleKey, String descKey, Predicate<String> onChange, boolean hideSeparator) {
        String text = SimplePreferences.getStringValue(textKey, defaultText);
        setupPreference(category, new SettingsEditItemController(
                text,
                null,
                LangHelper.getLang().getString(titleKey),
                descKey != null ? LangHelper.getLang().getString(descKey) : null,
                s -> {
                    if (onChange != null && !onChange.test(s)) {
                        return false;
                    }
                    SimplePreferences.putStringValue(textKey, s);
                    SimplePreferences.save();
                    return true;
                }, hideSeparator
        ).getRoot());
    }

    private void setupPreference(String category, Node node) {
        if (category == null) {
            throw new IllegalArgumentException("Category is null!");
        }
        if (!categoryContents.containsKey(category)) {
            throw new IllegalStateException("Category '" + category + "' is not initialized!");
        }
        categoryContents.get(category).add(node);
    }

    private void displayCategory(String category) {
        for (SettingsCategoryController controller : categoryControllers) {
            controller.setSelected(controller.getCategoryId().equals(category));
        }
        wrapper.getChildren().clear();
        wrapper.getChildren().addAll(categoryContents.get(category));
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

    private <T> Predicate<T> toTruePredicate(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return true;
        };
    }

    private <T> Predicate<T> toTruePredicate(Runnable runnable) {
        return t -> {
            runnable.run();
            return true;
        };
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
