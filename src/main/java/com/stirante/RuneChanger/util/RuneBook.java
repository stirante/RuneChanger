package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.RuneChanger;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.model.RunePage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RuneBook {
    private static HashMap<String, RunePage> availablePages = new HashMap<>();
    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    public static JFXListView<LocalPages.LocalPageCell> localPageListView;
    public static JFXListView<ClientPages.ClientPageCell> clientPageListView;

    public static void init(JFXListView<ClientPages.ClientPageCell> clientList, JFXListView<LocalPages.LocalPageCell> localList) {
        localPageListView = localList;
        clientPageListView = clientList;
    }

    public static class ClientPages {

        public static void refreshClientRunes(JFXListView<ClientPageCell> clientList) {
            availablePages.clear();
            availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
            clientList.getItems().clear();
            availablePages.values()
                    .forEach(availablePage -> clientList.getItems().add(createClientElement(availablePage)));
        }

        private static void handleImportButton(JFXButton button, Label label) {
            AtomicBoolean duplicate = new AtomicBoolean(false);
            localPageListView.getItems().filtered(var -> {
                Label l = (Label) var.getChildrenUnmodifiable().get(0);
                if (l.getText().equalsIgnoreCase(label.getText())) {
                    duplicate.set(true);
                }
                return true;
            });

            if (duplicate.get()) {
                ControllerUtil.getInstance()
                        .showInfo(LangHelper.getLang().getString("duplicate_name"), String.format(LangHelper.getLang()
                                .getString("duplicate_name_msg"), label.getText()));
                return;
            }

            RunePage runePage = availablePages.get(label.getText());
            if (runePage == null) {
                ControllerUtil.getInstance()
                        .showInfo(LangHelper.getLang().getString("missing_page"), LangHelper.getLang()
                                .getString("missing_page_message"));
                return;
            }

            SimplePreferences.addRuneBookPage(runePage);
            localPageListView.getItems().add(RuneBook.LocalPages.createLocalElement(runePage));
        }

        public static ClientPageCell createClientElement(RunePage page) {
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
            ClientPageCell cell = new ClientPageCell(label);
            return cell;
        }

        public static class ClientPageCell extends HBox {
            JFXButton button = new JFXButton();

            ClientPageCell(Label label) {
                super();
                label.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(label, Priority.ALWAYS);
                button.setText("Import");
                button.getStyleClass().add("runechanger-button");
                button.setRipplerFill(Color.TRANSPARENT);
                button.setOnAction(action -> RuneBook.ClientPages.handleImportButton(button, label));
                this.getChildren().addAll(label, button);
            }
        }
    }

    public static class LocalPages {

        public static void refreshLocalRunes(JFXListView<LocalPageCell> localList) {
            localList.getItems().clear();
            SimplePreferences.runeBookValues.forEach(page -> localList.getItems().add(createLocalElement(page)));
        }

        private static void handleDeleteButton(JFXButton button, Label label) {
            localPageListView.getItems().remove(label.getParent());
            SimplePreferences.removeRuneBookPage(label.getText());
        }

        private static void handleLoadButton(JFXButton button, Label label) {
            availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
            if (clientPageListView.getFocusModel().getFocusedItem() == null) {
                ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("no_runepage_to_load"),
                        LangHelper.getLang().getString("no_runepage_to_load_message"));
                return;
            }

            Label l = (Label) clientPageListView.getFocusModel().getFocusedItem().getChildrenUnmodifiable().get(0);
            Map<String, Integer> sortedPageIds = availablePages.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.parseInt(e.getValue().getSource())));
            Integer id = sortedPageIds.get(l.getText());
            if (id == null) {
                ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("no_runepage_to_overwrite"), LangHelper.getLang()
                        .getString("no_runepage_to_overwrite_message"));
                return;
            }
            new Thread(() -> {
                RunePage runePage =
                        SimplePreferences.getRuneBookPage(label.getText());
                if (runePage == null) {
                    ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("no_page_with_name"), String.format(LangHelper.getLang()
                            .getString("no_page_with_name_message"), label.getText()));
                    return;
                }
                RuneChanger.getInstance().getRunesModule().setCurrentRunePage(runePage);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
                Platform.runLater(() -> ClientPages.refreshClientRunes(clientPageListView));
            }).start();
        }

        public static LocalPageCell createLocalElement(RunePage page) {
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
            LocalPageCell cell = new LocalPageCell(label);
            return cell;
        }

        public static class LocalPageCell extends HBox {
            JFXButton deleteBtn = new JFXButton();
            JFXButton loadBtn = new JFXButton();

            LocalPageCell(Label label) {
                super();
                label.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(label, Priority.ALWAYS);
                deleteBtn.setText("Delete");
                deleteBtn.getStyleClass().add("runechanger-button");
                deleteBtn.setRipplerFill(Color.TRANSPARENT);
                deleteBtn.setOnAction(action -> RuneBook.LocalPages.handleDeleteButton(deleteBtn, label));
                loadBtn.setText("Load");
                loadBtn.getStyleClass().add("runechanger-button");
                loadBtn.setRipplerFill(Color.TRANSPARENT);
                loadBtn.setOnAction(action -> RuneBook.LocalPages.handleLoadButton(loadBtn, label));
                this.getChildren().addAll(label, loadBtn, deleteBtn);
            }
        }
    }


//    public static void handleCtrlV(JFXListView<Label> localList) {
//        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//        Transferable transferable = clipboard.getContents(null);
//        String data = null;
//        try {
//            data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
//        } catch (UnsupportedFlavorException | IOException e) {
//            e.printStackTrace();
//        }
//        if (data == null) {
//            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                    .getString("invalid_ctrl_c_buffer"), LangHelper.getLang().getString("only_runechanger_imports"));
//            return;
//        }
//        List<String> list = stringToList(data);
//        RunePage page = new RunePage();
//        if (!page.importRunePage(list)) {
//            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                    .getString("invalid_runepage"), "");
//            return;
//        }
//        if ((!localList.getItems().filtered(label -> label.getText().equalsIgnoreCase(page.getName())).isEmpty())) {
//            showWarning(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                    .getString("duplicate_name"), "");
//            return;
//        }
//        SimplePreferences.addRuneBookPage(page);
//        localList.getItems().add(createListElement(page));
//    }

//    public static void handleCtrlC(JFXListView<Label> selectedListView) {
//        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//        String selectedPage;
//        try {
//            selectedPage = selectedListView.getFocusModel().getFocusedItem().getText();
//        } catch (RuntimeException e) {
//            return;
//        }
//
//        RunePage runePage = SimplePreferences.getRuneBookPage(selectedPage);
//        if (runePage == null) {
//            ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("no_page_with_name"), String.format(LangHelper.getLang()
//                    .getString("no_page_with_name_message"), selectedListView
//                    .getFocusModel()
//                    .getFocusedItem()
//                    .getText()));
//            return;
//        }
//
//        if (!runePage.verify()) {
//            ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang().getString("invalid_runepage"));
//        }
//
//        StringSelection selection = new StringSelection(runePage.exportRunePage().toString());
//        clipboard.setContents(selection, selection);
//        ControllerUtil.getInstance().showInfo(LangHelper.getLang().getString("successful_rune_copy"), LangHelper.getLang()
//                .getString("successful_rune_copy"));
//    }

}