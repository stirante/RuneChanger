package com.stirante.runechanger.model.app;

import com.stirante.runechanger.api.Champion;

import java.util.List;
import java.util.StringJoiner;

public class CounterData {

    private List<Champion> strongAgainst;
    private List<Champion> weakAgainst;
    private List<Champion> strongWith;
    private List<Champion> weakWith;

    public List<Champion> getStrongAgainst() {
        return strongAgainst;
    }

    public CounterData setStrongAgainst(List<Champion> strongAgainst) {
        this.strongAgainst = strongAgainst;
        return this;
    }

    public List<Champion> getWeakAgainst() {
        return weakAgainst;
    }

    public CounterData setWeakAgainst(List<Champion> weakAgainst) {
        this.weakAgainst = weakAgainst;
        return this;
    }

    public List<Champion> getStrongWith() {
        return strongWith;
    }

    public CounterData setStrongWith(List<Champion> strongWith) {
        this.strongWith = strongWith;
        return this;
    }

    public List<Champion> getWeakWith() {
        return weakWith;
    }

    public CounterData setWeakWith(List<Champion> weakWith) {
        this.weakWith = weakWith;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CounterData.class.getSimpleName() + "[", "]")
                .add("strongAgainst=" + strongAgainst)
                .add("weakAgainst=" + weakAgainst)
                .add("strongWith=" + strongWith)
                .add("weakWith=" + weakWith)
                .toString();
    }
}
