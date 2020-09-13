package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.model.app.SettingsConfiguration;

import java.util.Map;

public interface Source {

    /**
     * Returns friendly name of the source
     */
    String getSourceName();

    /**
     * Returns settings key of the source
     */
    String getSourceKey();

    /**
     * Called when source settings are loaded or updated
     * @param settings
     */
    default void onSettingsUpdate(Map<String, Object> settings) {

    }

    /**
     * Called when creating settings GUI for source
     * @param config
     */
    default void setupSettings(SettingsConfiguration config) {

    }

}
