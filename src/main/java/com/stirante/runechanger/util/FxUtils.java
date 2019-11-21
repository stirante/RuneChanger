package com.stirante.runechanger.util;

import javafx.application.Platform;

public class FxUtils {

    public static void doOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(runnable);
        }
    }

}
