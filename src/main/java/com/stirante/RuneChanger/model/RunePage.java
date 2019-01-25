package com.stirante.RuneChanger.model;

import java.util.ArrayList;
import java.util.List;

public class RunePage {
    private String url;
    private String name;
    private Style mainStyle;
    private Style subStyle;
    private final List<Rune> runes = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();

    /**
     * Verifies rune page
     *
     * @return is rune page valid
     */
    public boolean verify() {
        if (getRunes().size() != 6) {
            return false;
        }
        for (Rune rune : runes) {
            if (rune == null) {
                return false;
            }
        }
        for (int i = 0; i < getRunes().size(); i++) {
            Rune rune = getRunes().get(i);
            if (i < 4 && rune.getStyle() != getMainStyle()) {
                System.out.println("Primary path contains runes from another style");
                return false;
            }
            else if (i >= 4 && rune.getStyle() != getSubStyle()) {
                System.out.println("Secondary path contains runes from another style");
                return false;
            }
            if (i < 4 && rune.getSlot() != i) {
                System.out.println("Rune does not belong to this slot");
                return false;
            }
            if (i == 4 && rune.getSlot() == getRunes().get(5).getSlot()) {
                System.out.println("Secondary path contains runes from the same slot");
                return false;
            }
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
                ", modifiers=" + modifiers +
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

    public List<Modifier> getModifiers() {
        return modifiers;
    }
}
