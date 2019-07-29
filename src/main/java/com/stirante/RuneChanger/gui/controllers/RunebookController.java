package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.util.ImageUtils;
import com.stirante.RuneChanger.util.RuneBook;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;

@Slf4j
public class RunebookController implements Initializable {

    @FXML
    private Circle championPortret;

    @FXML
    private JFXListView<Label> runeListView;

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
    }

    @FXML
    void onClientPagesButton(ActionEvent event) {
        RuneBook.refreshClientRunes(runeListView);
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
        new Thread(this::handleTextField).start();
    }

    private void handleTextField() {
        log.debug("Handle Text Field thread created");
        BorderPane pane = ControllerUtil.getInstance().getContentPane();
        while (pane.getCenter().equals(runebookPane)) {
            if (allChampionNames.contains(championTextField.getText())) {
                Champion champion = Champion.getByName(championTextField.getText());
                BufferedImage bufferedImage = ImageUtils.imageToBufferedImage(champion.getPortrait());
                Image m = SwingFXUtils.toFXImage(bufferedImage, null);
                championPortret.setFill(new ImagePattern(m));
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug("Handle text field thread destroyed");
    }


}
