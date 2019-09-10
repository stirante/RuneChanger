package com.stirante.runechanger.client;

import com.stirante.lolclient.ClientApi;
import generated.LolLootPlayerLoot;

import java.io.IOException;

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
}
