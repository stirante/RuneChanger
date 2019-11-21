package com.stirante.runechanger.util;

import javafx.application.Platform;

public abstract class AsyncTask<P, T, R> {

    @SafeVarargs
    public final void execute(P... params) {
        new Thread(() -> {
            try {
                R result = doInBackground(params);
                Platform.runLater(() -> onPostExecute(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError(e));
            }
        }).start();
    }

    public void onProgress(T progress) {

    }

    public final void publishProgress(T progress) {
        Platform.runLater(() -> onProgress(progress));
    }

    public abstract R doInBackground(P[] params);

    public void onPostExecute(R result) {

    }

    public void onError(Exception e) {

    }

}