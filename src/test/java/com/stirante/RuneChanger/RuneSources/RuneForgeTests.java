package com.stirante.RuneChanger.RuneSources;

import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.runestore.RuneforgeSource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class RuneForgeTests {

    private RuneforgeSource source;

    @Before
    public void init() throws IOException {
        Champion.init();
        source = new RuneforgeSource();
    }

    @Test
    public void isInitialized() {
        assertTrue(source.initialized);
    }

    @Test
    public void getChampionRunesTest() {
        Champion champion = Champion.getByName("Sivir");
        List<RunePage> pages = source.getForChampion(champion);
        assertTrue(pages.get(0).verify());
    }
}
