package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.client.ChampionsImpl;

public interface CounterSource extends Source {

    CounterData getCounterData(Champion champion);

}
