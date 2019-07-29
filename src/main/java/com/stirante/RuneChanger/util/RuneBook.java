package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.model.RunePage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.stirante.RuneChanger.gui.SettingsController.showInfoAlert;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import static com.stirante.RuneChanger.util.StringUtils.stringToList;

public class RuneBook {
    private static HashMap<String, RunePage> availablePages = new HashMap<>();
    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public static void refreshClientRunes(JFXListView<Label> runebookList) {
        availablePages.clear();
        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
        runebookList.getItems().clear();
        availablePages.values().forEach(availablePage -> runebookList.getItems().add(createListElement(availablePage)));
    }

    public static void refreshLocalRunes(JFXListView<Label> runebookList) {
        runebookList.getItems().clear();
        SimplePreferences.runeBookValues.forEach(page -> runebookList.getItems().add(createListElement(page)));
    }

    public static void handleCtrlV(JFXListView<Label> localList) {
        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
        Transferable transferable = clipboard.getContents(null);
        String data = null;
        try {
            data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
        if (data == null) {
            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
                    .getString("invalid_ctrl_c_buffer"), LangHelper.getLang().getString("only_runechanger_imports"));
            return;
        }
        List<String> list = stringToList(data);
        RunePage page = new RunePage();
        if (!page.importRunePage(list)) {
            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
                    .getString("invalid_runepage"), "");
            return;
        }
        if ((!localList.getItems().filtered(label -> label.getText().equalsIgnoreCase(page.getName())).isEmpty())) {
            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
                    .getString("duplicate_name"), "");
            return;
        }
        SimplePreferences.addRuneBookPage(page);
        localList.getItems().add(createListElement(page));
    }

    public static void handleCtrlC(JFXListView<Label> selectedListView) {
        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
        String selectedPage;
        try {
            selectedPage = selectedListView.getFocusModel().getFocusedItem().getText();
        } catch (RuntimeException e) {
            return;
        }

        RunePage runePage = SimplePreferences.getRuneBookPage(selectedPage);
        if (runePage == null) {
            showWarning(LangHelper.getLang().getString("no_page_with_name"), String.format(LangHelper.getLang()
                    .getString("no_page_with_name_message"), selectedListView
                    .getFocusModel()
                    .getFocusedItem()
                    .getText()), null);
            return;
        }

        if (!runePage.verify()) {
            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
                    .getString("invalid_runepage"), "");
        }

        StringSelection selection = new StringSelection(runePage.exportRunePage().toString());
        clipboard.setContents(selection, selection);
        showInfoAlert(LangHelper.getLang().getString("successful_rune_copy"), LangHelper.getLang()
                .getString("successful_rune_copy"), "");
    }

    public static void importLocalRunes(JFXListView<Label> localList, JFXListView<Label> clientList) {
        Label focusedItem = clientList.getFocusModel().getFocusedItem();
        if (focusedItem == null) {
            return;
        }
        if (!localList.getItems()
                .filtered(label -> label.getText().equalsIgnoreCase(focusedItem.getText()))
                .isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(LangHelper.getLang().getString("duplicate_name"));
            alert.setHeaderText(null);
            alert.setContentText(String.format(LangHelper.getLang()
                    .getString("duplicate_name_msg"), focusedItem.getText()));
            alert.showAndWait();
            return;
        }
        RunePage runePage = availablePages.get(focusedItem.getText());
        if (runePage == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(LangHelper.getLang().getString("missing_page"));
            alert.setHeaderText(null);
            alert.setContentText(LangHelper.getLang().getString("missing_page_message"));
            alert.showAndWait();
            return;
        }
        SimplePreferences.addRuneBookPage(runePage);
        localList.getItems().add(createListElement(runePage));
    }

    private static Label createListElement(RunePage page) {
        List<BufferedImage> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            BufferedImage image = page.getRunes().get(i).getImage();
            if (image == null) {
                return null;
            }
        }
        Label label = new Label(page.getName());
        label.setTextFill(Color.WHITE);
//        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
//        imageView.setFitHeight(25);
//        imageView.setFitWidth(25);
//        label.autosize();
//        label.setGraphic(imageView);
        return label;
    }

    public static void deleteRunePage(JFXListView<Label> localList) {
        Label selectedLabel = localList.getFocusModel().getFocusedItem();
        localList.getItems().remove(selectedLabel);
        SimplePreferences.removeRuneBookPage(selectedLabel.getText());
    }

    public static void loadAction(JFXListView<Label> localRuneList, JFXListView<Label> clientRunesList) {
        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
        if (localRuneList.getFocusModel().getFocusedItem() == null ||
                clientRunesList.getFocusModel().getFocusedItem() == null) {
            showWarning(LangHelper.getLang().getString("no_runepage_to_load"), LangHelper.getLang()
                    .getString("no_runepage_to_load_message"), null);
            return;
        }
        Map<String, Integer> sortedPageIds = availablePages.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.parseInt(e.getValue().getSource())));
        Integer id = sortedPageIds.get(clientRunesList.getFocusModel().getFocusedItem().getText());
        if (id == null) {
            showWarning(LangHelper.getLang().getString("no_runepage_to_overwrite"), LangHelper.getLang()
                    .getString("no_runepage_to_overwrite_message"), null);
            return;
        }
        new Thread(() -> {
            RunePage runePage =
                    SimplePreferences.getRuneBookPage(localRuneList.getFocusModel().getFocusedItem().getText());
            if (runePage == null) {
                showWarning(LangHelper.getLang().getString("no_page_with_name"), String.format(LangHelper.getLang()
                        .getString("no_page_with_name_message"), localRuneList
                        .getFocusModel()
                        .getFocusedItem()
                        .getText()), null);
                return;
            }
            RuneChanger.getInstance().getRunesModule().setCurrentRunePage(runePage);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
            Platform.runLater(() -> refreshClientRunes(clientRunesList));
        }).start();
    }

}