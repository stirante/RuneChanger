package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.util.FxUtils;
import javafx.collections.ObservableList;
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
import java.util.stream.Collectors;

public class ChampionGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);

    private final static String CHAMPION_URL = "https://champion.gg/champion/";
    private final String[] ROLES = {"Jungle", "Middle", "ADC", "Top", "Support"};

    private void extractRunePage(Document webPage, Champion champion, String role, ObservableList<RunePage> pages) {
        Elements elements = webPage.select("div.o-wrap");
        elements = elements.select("div.RuneBuilder__PathBody-dchrMz.bKqgWU");
        Element mainSide;
        Element secondarySide;
        try {
            mainSide = elements.first().child(0).child(0);
            secondarySide = elements.first().child(1).child(0);
        } catch (NullPointerException e) {
            return;
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
        page.setSource(webPage.baseUri());
        page.setChampion(champion);
        if (page.verify() && !pages.contains(page)) {
            FxUtils.doOnFxThread(() -> pages.add(page));
        }
    }

    private void extractRunes(Champion champion, ObservableList<RunePage> pages) {
        final String URL = CHAMPION_URL + champion.getInternalName() + "/";
        log.info("Visiting page to check for runes: " + URL);
        try {
            Document webPage = Jsoup.connect(URL).get();
            List<String> collect = webPage.select(".champion-profile ul li a h3")
                    .stream()
                    .map(Element::text)
                    .collect(Collectors.toList());
            String role = webPage.baseUri().replace(URL, "").replace("?", "");
            extractRunePage(webPage, champion, role, pages);
            for (String s : collect) {
                if (s.equalsIgnoreCase(role)) {
                    continue;
                }
                extractRunePage(webPage, champion, s, pages);
            }

        } catch (IOException e) {
            log.warn("ERROR RETRIEVING CHAMPION FROM Champion.gg! " + e);
        }
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
    public void getForChampion(Champion champion, ObservableList<RunePage> pages) {
        extractRunes(champion, pages);
    }

//    public static void main(String[] args) throws IOException {
//        Champion.init();
//
//        ChampionGGSource source = new ChampionGGSource();
//        Champion.values()
//                .forEach(champion -> System.out.println(
//                        source.getForChampion(champion).size() + " pages were found for " + champion.getName()));
//    }
}
