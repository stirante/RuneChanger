package com.stirante.runechanger.client;

import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.model.client.Champion;
import generated.LolChampionsCollectionsChampionMinimal;
import generated.LolCollectionsCollectionsChampionMastery;
import generated.LolLootPlayerLoot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class Loot extends ClientModule {
    public Loot(ClientApi api) {
        super(api);
    }

    /**
     * Crafts all the keys in hextech loot
     *
     * @return number of crafted keys or -1, when this method fails.
     */
    public int craftKeys() {
        try {
            LolLootPlayerLoot keyFragments = getApi()
                    .executeGet("/lol-loot/v1/player-loot/MATERIAL_key_fragment", LolLootPlayerLoot.class);
            if (keyFragments.count >= 3) {
                getApi().executePost("/lol-loot/v1/recipes/MATERIAL_key_fragment_forge/craft?repeat=" +
                        keyFragments.count / 3, new String[]{"MATERIAL_key_fragment"});
            }
            return keyFragments.count / 3;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void disenchantChampions() {
        try {
            LolLootPlayerLoot[] loot =
                    getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class);
            for (LolLootPlayerLoot item : loot) {
                if (item.lootId.startsWith("CHAMPION_RENTAL_")) {
                    for (int i = 0; i < item.count; i++) {
                        getApi().executePost("/lol-loot/v1/recipes/CHAMPION_RENTAL_disenchant/craft", new String[]{item.lootId});
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void smartDisenchantChampions() {
        try {
            LolChampionsCollectionsChampionMinimal[] champions =
                    getApi().executeGet("/lol-champions/v1/owned-champions-minimal", LolChampionsCollectionsChampionMinimal[].class);
            LolCollectionsCollectionsChampionMastery[] masteries =
                    getApi().executeGet("/lol-collections/v1/inventories/" + getCurrentSummoner().summonerId +
                            "/champion-mastery", LolCollectionsCollectionsChampionMastery[].class);
            LolLootPlayerLoot[] loot =
                    getApi().executeGet("/lol-loot/v1/player-loot", LolLootPlayerLoot[].class);

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
            e.printStackTrace();
        }
    }
}
