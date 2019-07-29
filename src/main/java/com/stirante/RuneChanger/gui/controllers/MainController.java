package com.stirante.RuneChanger.gui.controllers;

import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.gui.Settings;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.PathUtils;
import com.stirante.RuneChanger.util.SimplePreferences;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class MainController implements Initializable {

    @FXML
    private BorderPane border_pane;

    private Settings settings;

    public void init(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.debug("Runechanger is located in: " + PathUtils.getWorkingDirectory());
        log.info("Main Controller initializing");
        SimplePreferences.load();
        ControllerUtil.getInstance().setMainPane(border_pane);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setResources(LangHelper.getLang());
            fxmlLoader.setLocation(getClass().getResource("/fxml/ContentArea.fxml"));
            Parent contentArea = fxmlLoader.load();
            border_pane.setCenter(contentArea);
            ControllerUtil.getInstance().fade(contentArea, 1000, 0, 1).playFromStart();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
