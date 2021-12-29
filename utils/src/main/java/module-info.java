module utils {
    requires javafx.graphics;
    requires com.stirante.eventbus;
    requires org.slf4j;
    requires mslinks;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.desktop;
    requires com.google.gson;
    requires com.stirante.justpipe;

    exports com.stirante.runechanger.utils;
    opens com.stirante.runechanger.utils;

}