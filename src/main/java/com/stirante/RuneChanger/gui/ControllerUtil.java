package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.util.LangHelper;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class ControllerUtil {
    private final static ControllerUtil instance = new ControllerUtil();
    private BorderPane contentPane;

    public static ControllerUtil getInstance() {
        return instance;
    }

    public FXMLLoader getLoader(String fxmlPath) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(LangHelper.getLang());
        fxmlLoader.setLocation(getClass().getResource(fxmlPath));
        return fxmlLoader;
    }

    public <T> FadeTransition fade(T node, int duration, int from, int to) {
        FadeTransition ft = new FadeTransition(Duration.millis(duration), (Node) node);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    public BorderPane getContentPane() {
        return contentPane;
    }

    public void setContentPane(BorderPane p) {
        this.contentPane = p;
    }

}
