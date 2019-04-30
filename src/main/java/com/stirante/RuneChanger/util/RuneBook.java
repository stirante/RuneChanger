package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.lolclient.ClientApi;
import generated.LolPerksPerkPageResource;
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

import static com.stirante.RuneChanger.gui.SettingsController.showInfoAlert;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import static com.stirante.RuneChanger.util.StringUtils.stringToList;

public class RuneBook {
    private static HashMap<String, RunePage> availablePages = new HashMap<>();
    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public static void refreshClientRunes(JFXListView<Label> runebookList) {
        availablePages.clear();
        refreshAvailablePages();
        runebookList.getItems().clear();
        availablePages.values().forEach(availablePage -> runebookList.getItems().add(createListElement(availablePage)));
    }

    public static void refreshLocalRunes(JFXListView<Label> runebookList) {
        runebookList.getItems().clear();
        SimplePreferences.runeBookValues.forEach(page -> runebookList.getItems().add(createListElement(page)));
    }

    public static void handleCtrlV(JFXListView<Label> localList) {
        refreshAvailablePages();
        Transferable transferable = clipboard.getContents(null);
        String data = null;
        try {
            data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data == null) {
            showWarning("Error: Invalid data","Your CTRL-C buffer contains invalid data","You can only import runepages exported from Runechanger.");
            return;
        }
        List<String> list = stringToList(data);
        RunePage page = new RunePage();
        if (!page.importRunePage(list)) {
            showWarning("Invalid Runepage","Invalid Runepage","This runepage does not have the required information to be imported!");
            return;
        }
        //TODO: check for duplicate names
        if ((!localList.getItems().filtered(label -> label.getText().equalsIgnoreCase(page.getName())).isEmpty())) {
            showWarning("Invalid Runepage","Duplicate Name Error","Another runepage with the same name already exists");
            return;
        }
        SimplePreferences.addRuneBookPage(page);
        localList.getItems().add(createListElement(page));
    }

    public static void handleCtrlC(JFXListView<Label> selectedListView) {
        refreshAvailablePages();
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
            showWarning("Invalid Runepage","Error: Invalid Runepage","Please copy a valid runepage to continue.");
        }

        StringSelection selection = new StringSelection(runePage.exportRunePage().toString());
        clipboard.setContents(selection, selection);
        showInfoAlert("Succesfully copied Rune Data","You succesfully copied the selected Rune Data!","");
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
        BufferedImage image = page.getRunes().get(0).getImage();
        if (image == null) {
            return null;
        }
        Label label = new Label(page.getName());
        label.setTextFill(Color.WHITE);
        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
        imageView.setFitHeight(25);
        imageView.setFitWidth(25);
        label.autosize();
        label.setGraphic(imageView);
        return label;
    }

    public static void deleteRunePage(JFXListView<Label> localList) {
        Label selectedLabel = localList.getFocusModel().getFocusedItem();
        localList.getItems().remove(selectedLabel);
        SimplePreferences.removeRuneBookPage(selectedLabel.getText());
    }

    private static void refreshAvailablePages() {
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages;
            pages = RuneChanger.getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable && p.isValid) {
                    RunePage value = RunePage.fromClient(p);
                    if (value != null) {
                        availablePages.put(p.name, value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, Integer> getSortedPageIds() {
        HashMap<String, Integer> map = new HashMap<>();
        try {
            LolPerksPerkPageResource[] pages;
            pages = RuneChanger.getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable && p.isValid) {
                    RunePage value = RunePage.fromClient(p);
                    if (value != null) {
                        map.put(p.name, p.id);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void loadAction(JFXListView<Label> localRuneList, JFXListView<Label> clientRunesList) {
        ClientApi api = RuneChanger.getApi();
        refreshAvailablePages();
        LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
        if (localRuneList.getFocusModel().getFocusedItem() == null ||
                clientRunesList.getFocusModel().getFocusedItem() == null) {
            showWarning(LangHelper.getLang().getString("no_runepage_to_load"), LangHelper.getLang()
                    .getString("no_runepage_to_load_message"), null);
            return;
        }

        HashMap<String, Integer> sortedPageIds = getSortedPageIds();
        Integer id = sortedPageIds.get(clientRunesList.getFocusModel().getFocusedItem().getText());
        if (id == null) {
            showWarning(LangHelper.getLang().getString("no_runepage_to_overwrite"), LangHelper.getLang()
                    .getString("no_runepage_to_overwrite_message"), null);
            return;
        }
        new Thread(() -> {
            try {
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
                runePage.toClient(page1);

                api.executeDelete("/lol-perks/v1/pages/" + id);
                api.executePost("/lol-perks/v1/pages/", page1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshAvailablePages();
                Platform.runLater(() -> refreshClientRunes(clientRunesList));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}