package com.stirante.runechanger.client;

import com.stirante.runechanger.model.client.RunePage;
import com.stirante.lolclient.ClientApi;
import generated.LolPerksPerkPageResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Runes extends ClientModule {
    public Runes(ClientApi api) {
        super(api);
    }

    public void replaceRunePage (RunePage page, int id) {
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
            LolPerksPerkPageResource page1 = getApi().executeGet("/lol-perks/v1/pages/" + id, LolPerksPerkPageResource.class);
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
                page1 = availablePages.get(0);
            }
            page.toClient(page1);
            //updating rune page sometimes bugs out client, so we remove and add new one
            getApi().executeDelete("/lol-perks/v1/pages/" + page1.id);
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public HashMap<String, RunePage> getRunePages() {
        try {
            HashMap<String, RunePage> availablePages = new HashMap<>();
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable && p.isValid) {
                    RunePage value = RunePage.fromClient(p);
                    if (value != null) {
                        availablePages.put(p.name, value);
                    }
                }
            }
            return availablePages;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
