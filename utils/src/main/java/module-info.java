module utils {
    requires javafx.graphics;
    requires com.stirante.eventbus;
    requires lol.client.java.api;
    requires org.slf4j;
    requires mslinks;
    requires java.desktop;
    requires com.google.gson;
    requires com.stirante.justpipe;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    exports com.stirante.runechanger.utils;
    opens com.stirante.runechanger.utils;

}