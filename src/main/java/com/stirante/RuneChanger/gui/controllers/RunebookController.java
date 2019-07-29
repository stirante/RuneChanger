package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.util.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.TextFields;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
public class RunebookController implements Initializable {

    @FXML
    private Circle championPortret;

    @FXML
    private JFXListView<?> runeListView;

    @FXML
    private JFXButton localPagesButton;

    @FXML
    private JFXButton clientPagesButton;

    @FXML
    private JFXButton runeforgeButton;

    @FXML
    private TextField championTextField;

    @FXML
    private AnchorPane runebookPane;

    private static List<String> allChampionNames = new ArrayList<>();

    @FXML
    void handleTextFieldTyped(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER) || event.getCode().equals(KeyCode.ESCAPE)) {
            runebookPane.requestFocus(); //javafx textfield still has no UNFOCUS METHOD :(
        }
        if (allChampionNames.contains(championTextField.getText())) {
            Champion champion = Champion.getByName(championTextField.getText());
            BufferedImage bufferedImage = ImageUtils.imageToBufferedImage(champion.getPortrait());
            Image m = SwingFXUtils.toFXImage(bufferedImage, null);
            championPortret.setFill(new ImagePattern(m));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Runebook Controller initializing");
        Image m = new Image("\\images\\unknownchamp.png");
        championPortret.setFill(new ImagePattern(m));
        allChampionNames.clear();
        Champion.values().forEach(val -> {
            allChampionNames.add(val.getName());
        });
        TextFields.bindAutoCompletion(championTextField, allChampionNames);
    }


}
