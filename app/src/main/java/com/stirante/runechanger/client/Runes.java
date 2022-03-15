package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ApiResponse;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.api.RunePage;
import com.stirante.runechanger.utils.SimplePreferences;
import generated.LolPerksPerkPageResource;
import generated.LolPerksPlayerInventory;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Runes extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Runes.class);
    public static final String RUNE_PAGES_EVENT = "/lol-perks/v1/pages";

    public Runes(RuneChangerApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(CURRENT_SUMMONER_EVENT)
    public void onCurrentSummoner() {
        resetSummoner();
    }

    /**
     * Sets the current rune page for the player.
     * It will actually remove the rune page and add it again, because updating sometime bugs out the client.
     *
     * @param page The page to set
     */
    public void setCurrentRunePage(RunePage page) {
        if (page == null) {
            log.warn("Tried to set null rune page!");
            return;
        }
        if (Countly.isInitialized()) {
            Countly.session()
                    .event("rune_page_selection")
                    .addSegment("remote", String.valueOf(
                            page.getSource() == null || page.getSource().startsWith("http")))
                    .addSegment("source", page.getSourceName())
                    .record();
        }
        try {
            //change pages
            LolPerksPerkPageResource page1 =
                    getClientApi().executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class)
                            .getResponseObject();
            if (!page1.isEditable || !page1.isActive) {
                //get all rune pages
                LolPerksPerkPageResource[] pages =
                        getClientApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class)
                                .getResponseObject();
                //find available pages
                List<LolPerksPerkPageResource> availablePages =
                        Arrays.stream(pages).filter(p -> p.isEditable).collect(Collectors.toList());
                if (availablePages.size() > 0) {
                    page1 = availablePages.get(0);
                }
                else {
                    page1 = new LolPerksPerkPageResource();
                }
            }
            page.toClient(page1);
            //updating rune page sometimes bugs out client, so we remove and add new one
            if (page1.id != null) {
                getClientApi().executeDelete("/lol-perks/v1/pages/" + page1.id);
            }
            ApiResponse<Void> response = getClientApi().executePost("/lol-perks/v1/pages/", page1);
            if (!response.isOk()) {
                log.warn("Failed to add rune page! 'POST /lol-perks/v1/pages/' returned status code " +
                        response.getStatusCode());
                log.warn("Raw response: " + response.getRawResponse());
            }
        } catch (IOException ex) {
            log.error("Exception occurred while setting current rune page", ex);
        }
    }

    /**
     * Returns a list of rune pages from the client.
     *
     * @return
     */
    public List<RunePage> getRunePages() {
        List<RunePage> availablePages = new ArrayList<>();
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getClientApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class)
                            .getResponseObject();
            //find available pages
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    RunePage value = RunePage.fromClient(getApi(), p);
                    if (value != null) {
                        availablePages.add(value);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting all rune pages", e);
        }
        return availablePages;
    }

    /**
     * Returns the number of available rune pages.
     *
     * @return
     */
    public int getOwnedPageCount() {
        try {
            return getClientApi().executeGet("/lol-perks/v1/inventory", LolPerksPlayerInventory.class)
                    .getResponseObject().ownedPageCount;
        } catch (IOException e) {
            log.error("Exception occurred while getting owned rune page count", e);
        }
        return 0;
    }

    @Subscribe(RUNE_PAGES_EVENT)
    public void onRunePages() {
        // Auto sync rune pages to RuneChanger
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_SYNC, false)) {
            syncRunePages();
        }
    }

    /**
     * Deletes a rune page from client.
     *
     * @param page The page to delete.
     */
    public void deletePage(RunePage page) {
        try {
            getClientApi().executeDelete("/lol-perks/v1/pages/" + page.getSource());
        } catch (IOException e) {
            log.error("Exception occurred while deleting a rune page", e);
        }
    }

    /**
     * Adds a page to the client.
     *
     * @param page The page to add.
     */
    public void addPage(RunePage page) {
        LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
        page.toClient(page1);
        try {
            getClientApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException e) {
            log.error("Exception occurred while adding a rune page", e);
        }
    }

    /**
     * Syncs rune pages to RuneChanger.
     */
    public void syncRunePages() {
        List<ChampionBuild> savedPages = getApi().getRuneBook().getRuneBookValues();
        List<RunePage> clientPages = getRunePages();
        for (RunePage p : clientPages) {
            Optional<ChampionBuild> savedPage = savedPages.stream()
                    .filter(runePage -> runePage.getName().equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (savedPage.isPresent()) {
                savedPage.get().getRunePage().copyFrom(p);
            }
            else {
                getApi().getRuneBook().addRuneBookPage(p);
            }
        }
    }

}
