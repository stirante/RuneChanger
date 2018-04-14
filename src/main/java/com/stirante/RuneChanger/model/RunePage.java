package com.stirante.RuneChanger.model;

import java.util.ArrayList;
import java.util.List;

public class RunePage {
    private String url;
    private String name;
    private Style mainStyle;
    private Style subStyle;
    private List<Rune> runes = new ArrayList<>();

    /**
     * Verifies rune page
     * @return is rune page valid
     */
    public boolean verify() {
        if (getRunes().size() != 6) return false;
        for (int i = 0; i < getRunes().size(); i++) {
            Rune rune = getRunes().get(i);
            if (i < 4 && rune.getStyle() != getMainStyle()) return false;
            else if (i >= 4 && rune.getStyle() != getSubStyle()) return false;
            if (i < 4 && rune.getSlot() != i) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RunePage{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", mainStyle=" + mainStyle +
                ", subStyle=" + subStyle +
                ", runes=" + runes +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Style getMainStyle() {
        return mainStyle;
    }

    public void setMainStyle(Style mainStyle) {
        this.mainStyle = mainStyle;
    }

    public Style getSubStyle() {
        return subStyle;
    }

    public void setSubStyle(Style subStyle) {
        this.subStyle = subStyle;
    }

    public List<Rune> getRunes() {
        return runes;
    }
}
