package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ApiResponse;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.model.client.Champion;
import generated.LolChampionsCollectionsChampionMinimal;
import generated.LolCollectionsCollectionsChampionMastery;
import generated.LolLootPlayerLoot;
import generated.LolLootRecipeWithMilestones;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Loot extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Loot.class);

    /**
     * List of ignored materials, when searching for tokens. Currently it ignores keys, key fragments and honor orbs.
     */
    private static final List<String> IGNORED_MATERIALS =
            Arrays.asList("MATERIAL_329", "MATERIAL_key", "MATERIAL_key_fragment");

    public Loot(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(CURRENT_SUMMONER_EVENT)
    public void onCurrentSummoner() {
        resetSummoner();
    }

    /**
     * Crafts all the keys in hextech loot
     *
     * @return number of crafted keys or -1, when this method fails.
     */
    public int craftKeys() {
        try {
            LolLootPlayerLoot keyFragments = getApi()
                    .executeGet("/lol-loot/v1/player-loot/MATERIAL_key_fragment", LolLootPlayerLoot.class)
                    .getResponseObject();
            if (keyFragments.count >= 3) {
                getApi().executePost("/lol-loot/v1/recipes/MATERIAL_key_fragment_forge/craft?repeat=" +
                        keyFragments.count / 3, new String[]{"MATERIAL_key_fragment"});
            }
            return keyFragments.count / 3;
        } catch (IOException e) {
            log.error("Exception occurred while crafting keys", e);
            return -1;
        }
    }

    public void disenchantChampions() {
        try {
            LolLootPlayerLoot[] loot =
                    getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class).getResponseObject();
            for (LolLootPlayerLoot item : loot) {
                if (item.lootId.startsWith("CHAMPION_RENTAL_")) {
                    for (int i = 0; i < item.count; i++) {
                        getApi().executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while disenchanting champions", e);
        }
    }

    /**
     * Disenchant champion shards based on rules:
     * 1. Don't disenchant unowned champions
     * 2. Don't disenchant shards, if mastery is higher that 3 but less than 6
     * 3. Leave only one shards, if mastery is 6
     */
    public void smartDisenchantChampions() {
        try {
            LolChampionsCollectionsChampionMinimal[] champions =
                    getApi().executeGet("/lol-champions/v1/owned-champions-minimal", LolChampionsCollectionsChampionMinimal[].class)
                            .getResponseObject();
            LolCollectionsCollectionsChampionMastery[] masteries =
                    getApi().executeGet("/lol-collections/v1/inventories/" + getCurrentSummoner().summonerId +
                            "/champion-mastery", LolCollectionsCollectionsChampionMastery[].class).getResponseObject();
            LolLootPlayerLoot[] loot =
                    getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class).getResponseObject();

            // Loop through all loot items
            for (LolLootPlayerLoot item : loot) {
                // Check if it's champion shard
                if (item.lootId.startsWith("CHAMPION_RENTAL_")) {
                    Champion champion = Champion.getById(Integer.parseInt(item.lootId.replace("CHAMPION_RENTAL_", "")));
                    if (champion == null) {
                        continue;
                    }
                    // Check if player owns that champion
                    if (Arrays.stream(champions)
                            .anyMatch(owned -> owned.id == champion.getId() && owned.ownership.owned)) {
                        // Check if player has mastery for champion
                        Optional<LolCollectionsCollectionsChampionMastery> mastery = Arrays.stream(masteries)
                                .filter(m -> m.championId == champion.getId())
                                .findFirst();
                        if (mastery.isPresent()) {
                            // Player has mastery, check champion level
                            if (mastery.get().championLevel < 4 || mastery.get().championLevel == 7) {
                                // disenchant, because champion level is below 4 (champion not played often enough) or champion level is max
                                for (int i = 0; i < item.count; i++) {
                                    getApi().executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                                }
                            }
                            else if (mastery.get().championLevel == 6) {
                                // disenchant and leave only one shard, because player might need that one shard for mastery 7
                                for (int i = 0; i < item.count - 1; i++) {
                                    getApi().executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                                }
                            }
                        }
                        else {
                            // disenchant, because player doesn't have mastery for that champion
                            for (int i = 0; i < item.count; i++) {
                                getApi().executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while smart disenchanting champions", e);
        }
    }

    /**
     * Returns a map of loot Ids and pair of their name and count
     *
     * @return map of loot Id -> pair of loot name and count
     */
    public Map<String, Pair<String, Integer>> getEventTokens() {
        try {
            ApiResponse<LolLootPlayerLoot[]> loot =
                    getApi().executeGet("/lol-loot/v1/player-loot/", LolLootPlayerLoot[].class);
            return Arrays.stream(loot.getResponseObject())
                    .filter(lolLootPlayerLoot ->
                            lolLootPlayerLoot.type.equalsIgnoreCase("MATERIAL") &&
                                    !IGNORED_MATERIALS.contains(lolLootPlayerLoot.lootId))
                    .collect(Collectors.toMap(
                            lolLootPlayerLoot -> lolLootPlayerLoot.lootId,
                            lolLootPlayerLoot -> new Pair<>(lolLootPlayerLoot.localizedName, lolLootPlayerLoot.count))
                    );
        } catch (IOException e) {
            log.error("Exception occurred while getting map of event tokens", e);
            return new HashMap<>();
        }
    }

    /**
     * Returns a map of token recipe names and pair of their name and cost
     *
     * @param lootId loot id
     * @return map of token recipe name -> pair of recipe name and cost
     */
    public Map<String, Pair<String, Integer>> getRecipes(String lootId) {
        try {
            ApiResponse<LolLootRecipeWithMilestones[]> loot =
                    getApi().executeGet(
                            "/lol-loot/v1/recipes/initial-item/" + lootId, LolLootRecipeWithMilestones[].class);
            return Arrays.stream(loot.getResponseObject())
                    .filter(lolLootRecipe -> lolLootRecipe.slots.size() == 1 &&
                            lolLootRecipe.slots.get(0).lootIds.size() == 1 &&
                            lolLootRecipe.slots.get(0).lootIds.get(0).equalsIgnoreCase(lootId))
                    .collect(Collectors.toMap(
                            lolLootPlayerLoot -> lolLootPlayerLoot.recipeName,
                            lolLootPlayerLoot -> new Pair<>(lolLootPlayerLoot.description, lolLootPlayerLoot.slots.get(0).quantity))
                    );
        } catch (IOException e) {
            log.error("Exception occurred while getting map of recipes", e);
            return new HashMap<>();
        }
    }

    /**
     * Crafts a recipe
     *
     * @param recipeName recipe name
     * @param lootId     material Id to be consumed
     * @param repeat     how many times it should be crafted
     * @return whether the crafting was successful
     */
    public boolean craftRecipe(String recipeName, String lootId, int repeat) {
        try {
            ApiResponse<Void> response = getApi().executePost(
                    "/lol-loot/v1/recipes/" + recipeName + "/craft?repeat=" + repeat, new String[]{lootId});
            return response.isOk();
        } catch (IOException e) {
            log.error("Exception occurred while crafting a recipe", e);
            return false;
        }
    }

}
