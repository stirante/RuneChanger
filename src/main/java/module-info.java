module RuneChanger {
    requires java.base;
    requires java.scripting;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.controls;
    requires jdk.unsupported.desktop;
    requires java.naming;
    requires java.sql;
    requires jdk.crypto.ec;
    requires lol.client.java.api;
    requires com.jfoenix;
    requires slf4j.api;
    requires jna.platform;
    requires logback.classic;
    requires logback.core;
    requires org.jsoup;
    requires fuzzywuzzy;
    requires org.controlsfx.controls;
    requires jna;
    requires org.update4j;

    exports com.stirante.RuneChanger;
    exports com.stirante.RuneChanger.gui;
    exports com.stirante.RuneChanger.gui.controllers;
    exports com.stirante.RuneChanger.model.client;
    exports com.stirante.RuneChanger.client;
    exports com.stirante.RuneChanger.gui.components;
    opens com.stirante.RuneChanger.runestore;
    opens com.stirante.RuneChanger.model.client;
    opens com.stirante.RuneChanger.model.github;
    opens com.stirante.RuneChanger.model.log;
    opens com.stirante.RuneChanger.gui.components;
}