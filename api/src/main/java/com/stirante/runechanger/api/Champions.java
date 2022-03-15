package com.stirante.runechanger.api;

import java.util.List;

public interface Champions {

    /**
     * An event, that is triggered, when all champion portraits are present in the assets
     */
    String IMAGES_READY_EVENT = "IMAGES_READY_EVENT";

    /**
     * Return all champions
     *
     * @return unmodifiable list of all champions
     */
    List<Champion> getChampions();

    /**
     * Get champion by id
     *
     * @param id id
     * @return champion
     */
    Champion getById(int id);

    /**
     * Get champion by name
     *
     * @param name name
     * @return champion
     */
    Champion getByName(String name);

    /**
     * Get champion by name
     *
     * @param name             name
     * @param additionalChecks if set to true, there will be additional checks for champion name
     * @return champion
     */
    Champion getByName(String name, boolean additionalChecks);

    boolean areImagesReady();
}
