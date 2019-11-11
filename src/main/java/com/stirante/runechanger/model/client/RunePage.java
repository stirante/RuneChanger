package com.stirante.runechanger.model.client;

import generated.LolPerksPerkPageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RunePage {
    private static final Logger log = LoggerFactory.getLogger(RunePage.class);
    private final List<Rune> runes = new ArrayList<>(6);
    private final List<Modifier> modifiers = new ArrayList<>(3);
    private String source;
    private Champion champion;
    private boolean fromClient;
    private String name;
    private Style mainStyle;
    private Style subStyle;

    /**
     * Returns RunePage object based on page from client or null if page is wrong or incomplete
     *
     * @param page client rune page
     * @return RunePage object
     */
    public static RunePage fromClient(LolPerksPerkPageResource page) {
        // invalid page
        if (page.selectedPerkIds.size() != 9 || !page.isValid || !page.isEditable) {
            return null;
        }

        RunePage p = new RunePage();

        // copy simple values
        p.name = page.name;
        p.mainStyle = Style.getById(page.primaryStyleId);
        p.subStyle = Style.getById(page.subStyleId);
        p.source = String.valueOf(page.id);
        p.champion = null;
        p.fromClient = true;

        // copy selected runes
        for (int i = 0; i < 6; i++) {
            p.runes.add(Rune.getById(page.selectedPerkIds.get(i)));
        }

        // copy selected modifiers
        for (int i = 6; i < 9; i++) {
            p.modifiers.add(Modifier.getById(page.selectedPerkIds.get(i)));
        }

        // final verification
        if (!p.verify()) {
            return null;
        }

        return p;
    }

    /**
     * Verifies rune page
     *
     * @return true if rune page is valid
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
                log.error("Primary path contains runes from another style");
                return false;
            }
            else if (i >= 4 && rune.getStyle() != getSubStyle()) {
                log.error("Secondary path contains runes from another style");
                return false;
            }
            if (i < 4 && rune.getSlot() != i) {
                log.error("Rune does not belong to this slot");
                return false;
            }
            if (i == 4 && rune.getSlot() == getRunes().get(5).getSlot()) {
                log.error("Secondary path contains runes from the same slot");
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "RunePage{" +
                "source='" + source + '\'' +
                ", name='" + name + '\'' +
                ", mainStyle=" + mainStyle +
                ", subStyle=" + subStyle +
                ", runes=" + runes +
                ", modifiers=" + modifiers +
                '}';
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    private String toClientName(String name) {
        if (isFromClient()) {
            return name;
        }
        if (champion != null && !name.startsWith(this.champion.getName() + ":")) {
            name = this.champion.getName() + ":" + this.name;
        }
        // limit name to 25 characters (client limit)
        return name.substring(0, Math.min(25, name.length()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = toClientName(name);
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

    /**
     * Serializes object to binary format
     *
     * @param out output stream
     */
    public void serialize(DataOutputStream out) throws IOException {
        // version mark
        out.writeByte(0x2);

        // basic data
        out.writeUTF(name);
        out.writeInt(mainStyle.getId());
        out.writeInt(subStyle.getId());

        // list of runes
        out.writeInt(runes.size());
        for (Rune rune : runes) {
            out.writeInt(rune.getId());
        }

        // list of modifiers
        out.writeInt(modifiers.size());
        for (Modifier mod : modifiers) {
            out.writeInt(mod.getId());
        }

        if (champion != null) {
            out.writeInt(champion.getId());
        }
        else {
            out.writeInt(-1);
        }
    }

    /**
     * Deserializes object from binary format
     *
     * @param in input stream
     */
    public void deserialize(DataInputStream in) throws IOException {
        byte version = in.readByte();
        if (version >= 0x1) {
            name = in.readUTF();
            mainStyle = Style.getById(in.readInt());
            subStyle = Style.getById(in.readInt());

            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                runes.add(Rune.getById(in.readInt()));
            }

            size = in.readInt();
            for (int i = 0; i < size; i++) {
                modifiers.add(Modifier.getById(in.readInt()));
            }
        }
        if (version >= 0x2) {
            champion = Champion.getById(in.readInt());
        }
        else {
            log.warn("Unknown rune page version " + version);
        }
    }

    /**
     * Fills client rune page with new info
     *
     * @param page client rune page
     */
    public void toClient(LolPerksPerkPageResource page) {
        page.name = name;
        page.primaryStyleId = mainStyle.getId();
        page.subStyleId = subStyle.getId();
        if (page.selectedPerkIds == null) {
            page.selectedPerkIds = new ArrayList<>();
        }
        page.selectedPerkIds.clear();
        for (Rune rune : runes) {
            page.selectedPerkIds.add(rune.getId());
        }
        for (Modifier mod : modifiers) {
            page.selectedPerkIds.add(mod.getId());
        }
        page.isActive = true;
    }

    /**
     * Exports runepage into a list with numbers
     *
     * @return list containing rune and modifier id's
     */
    public String toSerializedString() {
        List<Object> runepageList = new ArrayList<>();

        runepageList.add(name);
        runepageList.add(mainStyle);
        runepageList.add(subStyle);

        runes.forEach(rune -> {
            runepageList.add(rune.getId());
        });

        modifiers.forEach(modifier -> {
            runepageList.add(modifier.getId());
        });

        return runepageList.toString();
    }

    /**
     * Imports runepage into a list with numbers.
     *
     * @param str rune page serialized to string
     * @return RunePage if successful null if not
     */
    public static RunePage fromSerializedString(String str) {
        try {
            RunePage page = new RunePage();
            String replace = str.replace("[", "");
            String replace1 = replace.replace("]", "");
            String[] parts = replace1.split(",", 2);
            String part1 = parts[0];
            replace1 = parts[1].replace(" ", "");
            replace1 = part1 + "," + replace1;
            List<String> runepageList = new ArrayList<>(Arrays.asList(replace1.split(",")));

            if (runepageList.size() != 12) {
                return null;
            }

            for (int i = 9; i < 12; i++) {
                page.modifiers.add(Modifier.getById(Integer.parseInt(runepageList.get(i))));
            }

            for (int i = 3; i < 9; i++) {
                page.runes.add(Rune.getById(Integer.parseInt(runepageList.get(i))));
            }

            page.name = runepageList.get(0);
            page.mainStyle = Style.valueOf(runepageList.get(1));
            page.subStyle = Style.valueOf(runepageList.get(2));
            return page.verify() ? page : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isFromClient() {
        return fromClient;
    }

    public void setFromClient(boolean value) {
        fromClient = value;
    }

    public void setChampion(Champion champion) {
        this.champion = champion;
        this.name = toClientName(name);
    }

    public Champion getChampion() {
        return champion;
    }

    //Maybe it will be useful someday
//    public RunePage copy() {
//        RunePage page = new RunePage();
//        page.name = name;
//        page.runes.addAll(runes);
//        page.modifiers.addAll(modifiers);
//        page.source = source;
//        page.champion = champion;
//        page.fromClient = fromClient;
//        page.mainStyle = mainStyle;
//        page.subStyle = subStyle;
//        return page;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RunePage runePage = (RunePage) o;
        return runes.equals(runePage.runes) &&
                modifiers.equals(runePage.modifiers) &&
                (champion == runePage.champion || isFromClient() != runePage.isFromClient()) &&
                name.equals(runePage.name) &&
                mainStyle == runePage.mainStyle &&
                subStyle == runePage.subStyle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(runes, modifiers, champion, name, mainStyle, subStyle);
    }

    public void copyFrom(RunePage page) {
        name = page.getName();
        mainStyle = page.mainStyle;
        subStyle = page.subStyle;
        runes.clear();
        runes.addAll(page.runes);
        modifiers.clear();
        modifiers.addAll(page.modifiers);
        champion = page.champion;
    }
}
