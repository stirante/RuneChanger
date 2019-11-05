package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChampionGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);

    private final static String CHAMPION_URL = "https://champion.gg/champion/";
    private final String[] ROLES = {"Jungle", "Middle", "ADC", "Top", "Support"};

    private List<RunePage> extractRunes(Champion champion) {
        List<RunePage> pages = new ArrayList<>();
        for (String role : ROLES) {
            final String URL = CHAMPION_URL + champion.getInternalName() + "/" + role;
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
                List<String> runepageList = new ArrayList<>();
                runepageList.add(champion.getName() + " - " + role);
                runepageList.add(Style.getByName(mainSide.child(0).text().substring(2)) + "");
                runepageList.add(Style.getByName(secondarySide.child(0).text().substring(2)) + "");
                for (Element e : mainSide.children()) {
                    String rune =
                            e.select(".iSYqxs.Slot__RightSide-bGHpkV > div:nth-of-type(1) > .hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH")
                                    .text();
                    if (rune.equals("")) {
                        continue;
                    }
                    runepageList.add(Objects.requireNonNull(Rune.getByName(rune)).getId() + "");
                }

                Element subRunesParent = secondarySide.child(1);
                Elements subRunes =
                        subRunesParent.select(".iSYqxs.Slot__RightSide-bGHpkV > .hGZpqL.Description__Block-bJdjrS > .eOLOWg.Description__Title-jfHpQH");
                for (Element e : subRunes) {
                    if (e.text().equals("")) {
                        continue;
                    }
                    runepageList.add(Objects.requireNonNull(Rune.getByName(e.text())).getId() + "");
                }

                Element modifierParent = secondarySide.child(2);
                Elements modifiers =
                        modifierParent.select(".iLoveCSS.iSYqxs.Slot__RightSide-bGHpkV > .statShardsOS.hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH");
                for (Element e : modifiers) {
                    runepageList.add(modifierConverter(e.text()) + "");
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
        return extractRunes(champion);
    }

    public static void main(String[] args) throws IOException {
        Champion.init();

        ChampionGGSource source = new ChampionGGSource();
        Champion.values()
                .forEach(champion -> System.out.println(
                        source.getForChampion(champion).size() + " pages were found for " + champion.getName()));
    }
}
