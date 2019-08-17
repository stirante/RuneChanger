package com.stirante.RuneChanger;

import com.stirante.lolclient.ClientApi;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Keep in mind that our test cases require you to have a connection open with the league api, so just keep your
 * league of legends launcher open while running them or expect them to fail.
 */

public class SetupApiConnection {

    public static ClientApi api = null;
    private static boolean setupIsDone = false;

    @BeforeClass
    public static void establishApiConnection() {
        if (setupIsDone) {
            return;
        }

        try {
            api = new ClientApi();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        int tries = 0;
        while (tries < 10) {
            if (!api.isConnected()) {
                System.out.println("Api not connected, waiting for 1 second.. Tries left: " + (10 - tries));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tries++;
        }

        if (!api.isConnected()) {
            Assert.fail();
        }
    }

    @Test
    public void apiIsConnected() throws IOException {
        Assert.assertTrue(api.isConnected());
        Assert.assertTrue(api.isAuthorized());
    }
}
