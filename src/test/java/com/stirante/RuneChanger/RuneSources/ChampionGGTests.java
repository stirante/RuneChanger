package com.stirante.RuneChanger.RuneSources;

import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.runestore.ChampionGGSource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ChampionGGTests {
    private ChampionGGSource source;

    @Before
    public void init() throws IOException {
        Champion.init();
        source = new ChampionGGSource();
    }

    @Test
    public void isInitialized() {
        assertTrue(source.isInitialized);
    }

    @Test
    public void getChampionRunesTest() {
        Champion champion = Champion.getByName("Sivir");
        List<RunePage> pages = source.getForChampion(champion);
        assertTrue(pages.get(0).verify());
    }
}
