package com.stirante.runechanger.model.client;

import java.util.Collection;
import java.util.List;

public class ChampionBuild {

    private final String source;
    private String sourceName;
    private final Champion champion;
    private final String name;
    private final RunePage runePage;
    private final SummonerSpell firstSpell;
    private final SummonerSpell secondSpell;

    private ChampionBuild(String source, String sourceName, Champion champion, String name, RunePage runePage, SummonerSpell firstSpell, SummonerSpell secondSpell) {
        this.source = source;
        this.sourceName = sourceName;
        this.champion = champion;
        this.name = name;
        this.runePage = runePage;
        this.firstSpell = firstSpell;
        this.secondSpell = secondSpell;
    }

    public String getSource() {
        return source;
    }

    public String getSourceName() {
        return sourceName;
    }

    public Champion getChampion() {
        return champion;
    }

    public String getName() {
        return name;
    }

    public RunePage getRunePage() {
        return runePage;
    }

    public SummonerSpell getFirstSpell() {
        return firstSpell;
    }

    public SummonerSpell getSecondSpell() {
        return secondSpell;
    }

    public boolean hasSummonerSpells() {
        return firstSpell != null && secondSpell != null;
    }

    public static Builder builder(RunePage runePage) {
        return new Builder(runePage);
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public static class Builder {
        private String source;
        private String sourceName;
        private Champion champion;
        private String name;
        private RunePage runePage;
        private SummonerSpell firstSpell;
        private SummonerSpell secondSpell;

        private Builder(RunePage runePage) {
            this.runePage = runePage;
            this.sourceName = runePage.getSourceName();
            this.source = runePage.getSource();
            this.name = runePage.getName();
            this.champion = runePage.getChampion();
        }

        public Builder withSpells(SummonerSpell firstSpell, SummonerSpell secondSpell) {
            this.firstSpell = firstSpell;
            this.secondSpell = secondSpell;
            return this;
        }

        public Builder withSpells(List<SummonerSpell> spells) {
            if (spells == null || spells.size() < 2) {
                return this;
            }
            this.firstSpell = spells.get(0);
            this.secondSpell = spells.get(1);
            return this;
        }

        public Builder withSource(String source, String sourceName) {
            this.source = source;
            this.sourceName = sourceName;
            return this;
        }

        public Builder withName(String name) {
            runePage.setName(name);
            this.name = runePage.getName();
            return this;
        }

        public Builder withRunePage(RunePage runePage) {
            this.runePage = runePage;
            this.name = runePage.getName();
            this.champion = runePage.getChampion();
            return this;
        }

        public Builder withChampion(Champion champion) {
            runePage.setChampion(champion);
            this.champion = champion;
            this.name = runePage.getName();
            return this;
        }

        public ChampionBuild create() {
            if (name == null || runePage == null) {
                throw new IllegalStateException("Builder does not have required arguments to be created!");
            }
            return new ChampionBuild(source, sourceName, champion, name, runePage, firstSpell, secondSpell);
        }

    }

}
