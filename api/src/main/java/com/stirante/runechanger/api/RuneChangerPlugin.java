package com.stirante.runechanger.api;

public abstract class RuneChangerPlugin {

    private final RuneChangerApi api;

    RuneChangerPlugin(RuneChangerApi api) {
        this.api = api;
    }

    public RuneChangerApi getApi() {
        return api;
    }

    public abstract void onEnable();


}
