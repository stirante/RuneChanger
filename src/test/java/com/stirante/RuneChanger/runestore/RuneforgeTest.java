package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.SetupApiConnection;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.lolclient.ClientApi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RuneforgeTest extends SetupApiConnection {

    private final String dummyChampionName = "Lee Sin";
    private ClientApi api = SetupApiConnection.api;
    private RuneforgeSource source;

    @Before
    public void testConnection() {
        source = new RuneforgeSource();
        Assert.assertTrue(source.isInitialized());
    }

    @Test
    public void verifyRuneSource() throws IOException {
        Champion.init();
        Champion champion = Champion.getByName(dummyChampionName);
        List<RunePage> runePages = new ArrayList();
        runePages = source.getForChampion(champion);
        Assert.assertFalse(runePages.isEmpty());
        RunePage runePage = runePages.get(0);
        Assert.assertTrue(runePage.verify());
    }

}
