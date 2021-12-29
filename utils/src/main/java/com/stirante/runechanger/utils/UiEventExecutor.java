package com.stirante.runechanger.utils;

import com.stirante.eventbus.EventExecutor;

import java.util.concurrent.CompletableFuture;

public class UiEventExecutor implements EventExecutor {
    @Override
    public CompletableFuture<Void> execute(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        FxUtils.doOnFxThread(() -> {
            if (runnable != null) {
                runnable.run();
            }
            future.complete(null);
        });
        return future;
    }
}
