package com.stirante.runechanger.sourcestore;

import com.stirante.runechanger.model.app.CounterData;
import com.stirante.runechanger.model.client.Champion;

public interface CounterSource extends Source {

    CounterData getCounterData(Champion champion);

}
