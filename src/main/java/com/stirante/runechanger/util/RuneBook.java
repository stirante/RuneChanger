package com.stirante.runechanger.util;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.gui.ControllerUtil;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.stirante.runechanger.util.StringUtils.stringToList;

public class RuneBook {
//    private static final Logger log = LoggerFactory.getLogger(RuneBook.class);
//    private static HashMap<String, RunePage> availablePages = new HashMap<>();
//    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//    public static JFXListView<LocalPages.LocalPageCell> localPageListView;
//    public static JFXListView<ClientPages.ClientPageCell> clientPageListView;
//    public static JFXListView<RuneBook.RuneSourcePages.RuneSourceCell> runeSourceListView;

//    public static void init(JFXListView<ClientPages.ClientPageCell> clientList, JFXListView<LocalPages.LocalPageCell> localList, JFXListView<RuneBook.RuneSourcePages.RuneSourceCell> sourceList) {
//        localPageListView = localList;
//        clientPageListView = clientList;
//        runeSourceListView = sourceList;
//        log.info("Runebook initialized!");
//    }

//    public static void handleCtrlV(JFXListView<RuneBook.LocalPages.LocalPageCell> localList) {
//        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//        Transferable transferable = clipboard.getContents(null);
//        String data = null;
//        try {
//            data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
//        } catch (UnsupportedFlavorException | IOException e) {
//            e.printStackTrace();
//        }
//
//        log.info("Trying CTRL V action, clipboard contents: " + data);
//        if (data == null) {
//            ControllerUtil
//                    .showInfo(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                            .getString("invalid_ctrl_c_buffer"));
//            return;
//        }
//
//        RunePage page = new RunePage();
//        if (!page.fromSerializedString(data)) {
//            ControllerUtil
//                    .showInfo(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                            .getString("invalid_runepage"));
//            return;
//        }
//
//        AtomicBoolean duplicate = new AtomicBoolean(false);
//        localPageListView.getItems().filtered(var -> {
//            if (var.text.getText().equalsIgnoreCase(page.getName())) {
//                duplicate.set(true);
//            }
//            return true;
//        });
//
//        if (duplicate.get()) {
//            ControllerUtil
//                    .showInfo(LangHelper.getLang().getString("duplicate_name"), String.format(LangHelper.getLang()
//                            .getString("duplicate_name_msg"), page.getName()));
//            return;
//        }
//
//        SimplePreferences.addRuneBookPage(page);
//        localList.getItems().add(RuneBook.LocalPages.createLocalElement(page));
//    }
//
//    public static void handleCtrlC(JFXListView<RuneBook.LocalPages.LocalPageCell> listView) {
//        availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//        String selectedPage = listView.getFocusModel().getFocusedItem().text.getText();
//        RunePage runePage = SimplePreferences.getRuneBookPage(selectedPage);
//        if (runePage == null) {
//            ControllerUtil
//                    .showInfo(LangHelper.getLang().getString("no_page_with_name"), String.format(LangHelper.getLang()
//                            .getString("no_page_with_name_message"), listView
//                            .getFocusModel()
//                            .getFocusedItem()
//                            .text.getText()));
//            return;
//        }
//
//        if (!runePage.verify()) {
//            ControllerUtil
//                    .showInfo(LangHelper.getLang().getString("invalid_runepage"), LangHelper.getLang()
//                            .getString("invalid_runepage"));
//        }
//
//        StringSelection selection = new StringSelection(runePage.toSerializedString());
//        clipboard.setContents(selection, selection);
//        ControllerUtil
//                .showInfo(LangHelper.getLang().getString("successful_rune_copy"), LangHelper.getLang()
//                        .getString("successful_rune_copy"));
//    }
//
//    public static class ClientPages {
//
//        public static void refreshClientRunes(JFXListView<ClientPageCell> clientList) throws IllegalStateException {
//            availablePages.clear();
//            availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//            clientList.getItems().clear();
//            availablePages.values()
//                    .forEach(availablePage -> clientList.getItems().add(createClientElement(availablePage)));
//        }
//
//        private static void handleImportButton(JFXButton button, Label label) {
//            AtomicBoolean duplicate = new AtomicBoolean(false);
//            localPageListView.getItems().filtered(var -> {
//                if (var.text.getText().equalsIgnoreCase(label.getText())) {
//                    duplicate.set(true);
//                }
//                return true;
//            });
//
//            if (duplicate.get()) {
//                ControllerUtil
//                        .showInfo(LangHelper.getLang().getString("duplicate_name"), String.format(LangHelper.getLang()
//                                .getString("duplicate_name_msg"), label.getText()));
//                return;
//            }
//
//            RunePage runePage = availablePages.get(label.getText());
//            if (runePage == null) {
//                ControllerUtil
//                        .showInfo(LangHelper.getLang().getString("missing_page"), LangHelper.getLang()
//                                .getString("missing_page_message"));
//                return;
//            }
//
//            SimplePreferences.addRuneBookPage(runePage);
//            localPageListView.getItems().add(RuneBook.LocalPages.createLocalElement(runePage));
//            log.info("Imported runepage: " + runePage.getName());
//        }
//
//        public static ClientPageCell createClientElement(RunePage page) {
//            BufferedImage image = page.getRunes().get(0).getImage();
//            if (image == null) {
//                return null;
//            }
//            Label label = new Label(page.getName());
//            label.setTextFill(Color.WHITE);
//            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
//            imageView.setFitHeight(25);
//            imageView.setFitWidth(25);
//            label.autosize();
//            label.setGraphic(imageView);
//            ClientPageCell cell = new ClientPageCell(label);
//            return cell;
//        }
//
//        public static class ClientPageCell extends HBox {
//            JFXButton button = new JFXButton();
//            Label text;
//
//            ClientPageCell(Label label) {
//                super();
//                text = label;
//                label.setMaxWidth(Double.MAX_VALUE);
//                HBox.setHgrow(label, Priority.ALWAYS);
//                button.setText("Export");
//                button.getStyleClass().add("runechanger-button");
//                button.setRipplerFill(Color.TRANSPARENT);
//                button.setOnAction(action -> RuneBook.ClientPages.handleImportButton(button, label));
//                this.getChildren().addAll(label, button);
//            }
//        }
//    }
//
//    public static class LocalPages {
//
//        public static void refreshLocalRunes(JFXListView<LocalPageCell> localList) {
//            localList.getItems().clear();
//            SimplePreferences.getRuneBookValues().forEach(page -> localList.getItems().add(createLocalElement(page)));
//        }
//
//        private static void handleDeleteButton(JFXButton button, Label label) {
//            localPageListView.getItems().remove(label.getParent());
//            SimplePreferences.removeRuneBookPage(label.getText());
//        }
//
//        private static void handleLoadButton(JFXButton button, Label label) {
//            availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//            if (clientPageListView.getFocusModel().getFocusedItem() == null) {
//                ControllerUtil.showInfo(LangHelper.getLang().getString("no_runepage_to_load"),
//                        LangHelper.getLang().getString("no_runepage_to_load_message"));
//                return;
//            }
//
//            Label l = clientPageListView.getFocusModel().getFocusedItem().text;
//            Map<String, Integer> sortedPageIds = availablePages.entrySet()
//                    .stream()
//                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.parseInt(e.getValue().getSource())));
//            Integer id = sortedPageIds.get(l.getText());
//            if (id == null) {
//                ControllerUtil
//                        .showInfo(LangHelper.getLang().getString("no_runepage_to_overwrite"), LangHelper.getLang()
//                                .getString("no_runepage_to_overwrite_message"));
//                return;
//            }
//
//            new Thread(() -> {
//                RunePage runePage =
//                        SimplePreferences.getRuneBookPage(label.getText());
//                if (runePage == null) {
//                    ControllerUtil
//                            .showInfo(LangHelper.getLang()
//                                    .getString("no_page_with_name"), String.format(LangHelper.getLang()
//                                    .getString("no_page_with_name_message"), label.getText()));
//                    return;
//                }
//                RuneChanger.getInstance().getRunesModule().replaceRunePage(runePage, id);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                availablePages = RuneChanger.getInstance().getRunesModule().getRunePages();
//                Platform.runLater(() -> ClientPages.refreshClientRunes(clientPageListView));
//                log.info("Replaced runepage id: " + id + " with " + runePage.getName());
//            }).start();
//
//        }
//
//        public static LocalPageCell createLocalElement(RunePage page) {
//            BufferedImage image = page.getRunes().get(0).getImage();
//            if (image == null) {
//                return null;
//            }
//            Label label = new Label(page.getName());
//            label.setTextFill(Color.WHITE);
//            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
//            imageView.setFitHeight(25);
//            imageView.setFitWidth(25);
//            label.autosize();
//            label.setGraphic(imageView);
//            LocalPageCell cell = new LocalPageCell(label);
//            return cell;
//        }
//
//        public static class LocalPageCell extends HBox {
//            JFXButton deleteBtn = new JFXButton();
//            JFXButton loadBtn = new JFXButton();
//            Label text;
//
//            LocalPageCell(Label label) {
//                super();
//                text = label;
//                label.setMaxWidth(Double.MAX_VALUE);
//                HBox.setHgrow(label, Priority.ALWAYS);
//                deleteBtn.setText("Delete");
//                deleteBtn.getStyleClass().add("runechanger-button");
//                deleteBtn.setRipplerFill(Color.TRANSPARENT);
//                deleteBtn.setOnAction(action -> RuneBook.LocalPages.handleDeleteButton(deleteBtn, label));
//                loadBtn.setText("Import");
//                loadBtn.getStyleClass().add("runechanger-button");
//                loadBtn.setRipplerFill(Color.TRANSPARENT);
//                loadBtn.setOnAction(action -> RuneBook.LocalPages.handleLoadButton(loadBtn, label));
//                this.getChildren().addAll(label, loadBtn, deleteBtn);
//            }
//        }
//    }
//
//    public static class RuneSourcePages {
//
//        private static List<RunePage> downloadedPages;
//
//        public static void refreshRuneSourcePages(Champion selectedChampion, List<RunePage> list) {
//            if (selectedChampion.getName() == null || selectedChampion.getName() == "") {
//                return;
//            }
//            RuneBook.runeSourceListView.getItems().clear();
//            downloadedPages = list;
//            downloadedPages.forEach(page -> RuneBook.runeSourceListView.getItems().add(createLocalElement(page)));
//        }
//
//        public static void importRuneSourcePage(JFXButton button, Label label) {
//            AtomicBoolean duplicate = new AtomicBoolean(false);
//            localPageListView.getItems().filtered(var -> {
//                if (var.text.getText().equalsIgnoreCase(label.getText())) {
//                    duplicate.set(true);
//                }
//                return true;
//            });
//            if (duplicate.get()) {
//                ControllerUtil
//                        .showInfo(LangHelper.getLang().getString("duplicate_name"), String.format(LangHelper.getLang()
//                                .getString("duplicate_name_msg"), label.getText()));
//                return;
//            }
//
//            final RunePage[] runePage = new RunePage[1];
//            downloadedPages.forEach(var -> {
//                if (var.getName().equalsIgnoreCase(label.getText())) {
//                    runePage[0] = var;
//                }
//            });
//
//            if (runePage[0] == null) {
//                ControllerUtil
//                        .showInfo(LangHelper.getLang().getString("missing_page"), LangHelper.getLang()
//                                .getString("missing_page_message"));
//                return;
//            }
//
//            SimplePreferences.addRuneBookPage(runePage[0]);
//            localPageListView.getItems().add(RuneBook.LocalPages.createLocalElement(runePage[0]));
//            log.info("Imported runepage: " + runePage[0].getName());
//        }
//
//        private static RuneSourceCell createLocalElement(RunePage page) {
//            BufferedImage image = page.getRunes().get(0).getImage();
//            if (image == null) {
//                return null;
//            }
//            Label label = new Label(page.getName());
//            label.setTextFill(Color.WHITE);
//            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
//            imageView.setFitHeight(25);
//            imageView.setFitWidth(25);
//            label.autosize();
//            label.setGraphic(imageView);
//            RuneSourceCell cell = new RuneSourceCell(label);
//            return cell;
//        }
//
//        public static class RuneSourceCell extends HBox {
//            JFXButton importBtn = new JFXButton();
//            Label text;
//
//            RuneSourceCell(Label label) {
//                super();
//                text = label;
//                label.setMaxWidth(Double.MAX_VALUE);
//                HBox.setHgrow(label, Priority.ALWAYS);
//                importBtn.setText("Import");
//                importBtn.getStyleClass().add("runechanger-button");
//                importBtn.setRipplerFill(Color.TRANSPARENT);
//                importBtn.setOnAction(action -> RuneBook.RuneSourcePages.importRuneSourcePage(importBtn, label));
//                this.getChildren().addAll(label, importBtn);
//            }
//        }
//    }

}