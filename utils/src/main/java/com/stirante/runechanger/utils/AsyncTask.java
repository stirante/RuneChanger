package com.stirante.runechanger.utils;

import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncTask<P, T, R> {

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    @SafeVarargs
    public final void execute(P... params) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                R result = doInBackground(params);
                Platform.runLater(() -> onPostExecute(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError(e));
            }
        });
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