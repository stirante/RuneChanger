package com.stirante.RuneChanger.util;

import com.stirante.RuneChanger.RuneChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class temp {
    public static void doSomething() {
        Logger logger = LoggerFactory.getLogger(temp.class);
        System.out.println("level: " + logger.isDebugEnabled());
        logger.debug("debug");
        logger.warn("warn");
    }
}
