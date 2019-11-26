package com.stirante.runechanger.client;

import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.FxUtils;
import com.stirante.runechanger.util.SimplePreferences;
import generated.LolPerksPerkPageResource;
import generated.LolPerksPlayerInventory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class Runes extends ClientModule {

    private final HashSet<Runnable> onPageChange = new HashSet<>();

    public Runes(ClientApi api) {
        super(api);
    }

    public void replaceRunePage(RunePage page, int id) {
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            ArrayList<LolPerksPerkPageResource> availablePages = new ArrayList<>();
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    availablePages.add(p);
                }
            }
            //change pages
            LolPerksPerkPageResource page1 =
                    getApi().executeGet("/lol-perks/v1/pages/" + id, LolPerksPerkPageResource.class);
            if (!page1.isEditable || !page1.isActive) {
                page1 = availablePages.get(0);
            }
            page.toClient(page1);
            //updating rune page sometimes bugs out client, so we remove and add new one
            getApi().executeDelete("/lol-perks/v1/pages/" + id);
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void setCurrentRunePage(RunePage page) {
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            ArrayList<LolPerksPerkPageResource> availablePages = new ArrayList<>();
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    availablePages.add(p);
                }
            }
            //change pages
            LolPerksPerkPageResource page1 =
                    getApi().executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class);
            if (!page1.isEditable || !page1.isActive) {
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
                getApi().executeDelete("/lol-perks/v1/pages/" + page1.id);
            }
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<RunePage> getRunePages() {
        List<RunePage> availablePages = new ArrayList<>();
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    RunePage value = RunePage.fromClient(p);
                    if (value != null) {
                        availablePages.add(value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return availablePages;
    }

    public int getOwnedPageCount() {
        try {
            return getApi().executeGet("/lol-perks/v1/inventory", LolPerksPlayerInventory.class).ownedPageCount;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void handlePageChange(LolPerksPerkPageResource[] pages) {
        // Auto sync rune pages to RuneChanger
        if (SimplePreferences.getValue(SimplePreferences.SettingsKeys.AUTO_SYNC, false)) {
            syncRunePages();
        }

        for (Runnable runnable : onPageChange) {
            if (runnable != null) {
                try {
                    FxUtils.doOnFxThread(runnable);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public void addOnPageChangeListener(Runnable runnable) {
        onPageChange.add(runnable);
    }

    public void removeOnPageChangeListener(Runnable runnable) {
        onPageChange.remove(runnable);
    }

    public void deletePage(RunePage page) {
        try {
            getApi().executeDelete("/lol-perks/v1/pages/" + page.getSource());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPage(RunePage page) {
        LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
        page.toClient(page1);
        try {
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncRunePages() {
        ArrayList<RunePage> savedPages = SimplePreferences.getRuneBookValues();
        List<RunePage> clientPages = getRunePages();
        for (RunePage p : clientPages) {
            Optional<RunePage> savedPage = savedPages.stream()
                    .filter(runePage -> runePage.getName().equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (savedPage.isPresent()) {
                savedPage.get().copyFrom(p);
            }
            else {
                SimplePreferences.addRuneBookPage(p);
            }
        }
    }

}
