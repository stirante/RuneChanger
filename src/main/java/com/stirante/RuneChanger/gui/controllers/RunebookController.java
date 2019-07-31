package com.stirante.RuneChanger.gui.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.runestore.RuneforgeSource;
import com.stirante.RuneChanger.util.ImageUtils;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.RuneBook;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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
    private JFXListView<RuneBook.LocalPages.LocalPageCell> localPageView;

    @FXML
    private JFXListView<RuneBook.ClientPages.ClientPageCell> clientPageView;

    @FXML
    private JFXListView<RuneBook.RuneSourcePages.RuneSourceCell> runeSourcePageView;

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

    @FXML
    private JFXSpinner progressSpinner;


    private static List<String> allChampionNames = new ArrayList<>();
    public static Champion currentChosenChampion = null;

    @FXML
    void handleTextFieldTyped(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER) || event.getCode().equals(KeyCode.ESCAPE)) {
            runebookPane.requestFocus(); //since the textfield does not have a unfocus method this will have to do
        }
    }

    @FXML
    void onRuneSourceButton(ActionEvent event) {
        if (currentChosenChampion == null) {
            ControllerUtil.getInstance()
                    .showInfo("No champion selected!", "Please select a champion in the top right first before doing this!");
            return;
        }
        localPageView.setVisible(false);
        runeSourcePageView.setVisible(true);
        progressSpinner.setVisible(true);
        runeSourcePageView.getItems().clear();
        new Thread(() -> {
            RuneforgeSource runeforgeSource = new RuneforgeSource();
            List<RunePage> list = runeforgeSource.getForChampion(currentChosenChampion);
            Platform.runLater(() -> {
                RuneBook.RuneSourcePages.refreshRuneSourcePages(currentChosenChampion, list);
                progressSpinner.setVisible(false);
            });
        }).start();
    }

    @FXML
    void onClientPagesButton(ActionEvent event) {
        try {
            RuneBook.ClientPages.refreshClientRunes(clientPageView);
        } catch (IllegalStateException e) {
            ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("client_not_connected_title"),
                    LangHelper.getLang().getString("client_not_connected_message"));
        }
    }

    @FXML
    void onListViewKeyPressed(KeyEvent event) {
        if (event.getCode().equals(KeyCode.C) && event.isControlDown()) {
            RuneBook.handleCtrlC(localPageView);
        }
        else if (event.getCode().equals(KeyCode.V) && event.isControlDown()) {
            RuneBook.handleCtrlV(localPageView);
        }
    }

    @FXML
    void onLocalPagesButton(ActionEvent event) {
        localPageView.setVisible(true);
        runeSourcePageView.setVisible(false);
        RuneBook.LocalPages.refreshLocalRunes(localPageView);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Runebook Controller initializing");
        Image m = new Image("/images/unknownchamp.png");
        championPortret.setFill(new ImagePattern(m));
        allChampionNames.clear();
        Champion.values().forEach(val -> {
            allChampionNames.add(val.getName());
        });
        TextFields.bindAutoCompletion(championTextField, allChampionNames);
        Platform.runLater(() -> {
            RuneBook.init(clientPageView, localPageView, runeSourcePageView);
            RuneBook.LocalPages.refreshLocalRunes(localPageView);
            new Thread(this::handleTextField).start();
        });
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
                currentChosenChampion = champion;
            }
            else if (currentChosenChampion != null) {
                currentChosenChampion = null;
                Image m = new Image("/images/unknownchamp.png");
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
