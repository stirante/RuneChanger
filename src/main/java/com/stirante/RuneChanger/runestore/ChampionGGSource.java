package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ChampionGGSource implements RuneSource {

    private final String BASEURL = "https://champion.gg";
    private final String CHAMPIONURL = "https://champion.gg/champion/";
    private final String[] ROLES = {"Jungle", "Middle", "ADC", "Top", "Support"};
    public boolean isInitialized = false;

    public ChampionGGSource() throws IOException {
        Jsoup.connect(BASEURL).get(); //test connection
        isInitialized = true;
    }

    private List<RunePage> extractRunes(Champion champion) {
        List<RunePage> pages = new ArrayList();
        for (String role : ROLES) {
            final String URL = CHAMPIONURL + champion.getInternalName() + "/" + role;
            log.info("Visiting page to check for runes: " + URL);
            try {
                Document webPage = Jsoup.connect(URL).get();
                if (webPage.baseUri().contains("?")) {
                    continue;
                }
                Elements elements = webPage.select("div.o-wrap");
                elements = elements.select("div.RuneBuilder__PathBody-dchrMz.bKqgWU");
                Element mainSide;
                Element secondarySide;
                try {
                    mainSide = elements.first().child(0).child(0);
                    secondarySide = elements.first().child(1).child(0);
                } catch (NullPointerException e) {
                    continue;
                }
                List runepageList = new ArrayList();
                runepageList.add(champion.getName() + " - " + role);
                runepageList.add(Style.getByName(mainSide.child(0).text().substring(2)).toString());
                runepageList.add(Style.getByName(secondarySide.child(0).text().substring(2)).toString());
                for (Element e : mainSide.children()) {
                    String rune =
                            e.select(".iSYqxs.Slot__RightSide-bGHpkV > div:nth-of-type(1) > .hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH")
                                    .text();
                    if (rune.equals("")) {
                        continue;
                    }
                    runepageList.add(Integer.toString(Rune.getByName(rune).getId()));
                }

                Element subRunesParent = secondarySide.child(1);
                Elements subRunes =
                        subRunesParent.select(".iSYqxs.Slot__RightSide-bGHpkV > .hGZpqL.Description__Block-bJdjrS > .eOLOWg.Description__Title-jfHpQH");
                for (Element e : subRunes) {
                    if (e.text().equals("")) {
                        continue;
                    }
                    runepageList.add(Integer.toString(Rune.getByName(e.text()).getId()));
                }

                Element modifierParent = secondarySide.child(2);
                Elements modifiers = modifierParent.select(".iLoveCSS.iSYqxs.Slot__RightSide-bGHpkV > .statShardsOS.hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH");
                for (Element e : modifiers) {
                    runepageList.add(modifierConverter(e.text()).toString());
                }

                RunePage page = new RunePage();
                page.importRunePage(runepageList);
                page.setSource("Champion.GG");
                if (page.verify() && !pages.contains(page)) {
                    pages.add(page);
                }

            } catch (IOException e) {
                log.warn("ERROR RETRIEVING CHAMPION FROM Champion.gg! " + e);
            }
        }
        return pages;
    }

    private Integer modifierConverter(String modifierName) {
        switch (modifierName.toLowerCase()) {
            case "attack speed":
                return Modifier.RUNE_5005.getId();
            case "adaptive force":
                return Modifier.RUNE_5008.getId();
            case "armor":
                return Modifier.RUNE_5002.getId();
            case "magic resist":
                return Modifier.RUNE_5003.getId();
            case "scaling cooldown reduction":
                return Modifier.RUNE_5007.getId();
            case "scaling health":
                return Modifier.RUNE_5001.getId();
            default:
                return null;
        }
    }

    @Override
    public String getSourceName() {
        return "Champion.gg";
    }

    @Override
    public List<RunePage> getForChampion(Champion champion) {
        List<RunePage> list = extractRunes(champion);
        return list;
    }

    public static void main(String args[]) throws IOException {
        Champion.init();
        ChampionGGSource source = new ChampionGGSource();
        Champion.values()
                .forEach(champion -> System.out.println(
                        source.getForChampion(champion).size() + " pages were found for " + champion.getName()));
    }
}
