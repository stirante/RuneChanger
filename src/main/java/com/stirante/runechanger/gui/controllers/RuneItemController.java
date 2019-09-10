package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.LangHelper;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class RuneItemController {

    public Pane container;
    public ImageView icon;
    public Label name;
    public CheckBox selected;
    public ImageView delete;
    public ImageView edit;
    public Label shortName;
    public Label source;
    public ImageView importPage;
    private List<Node> newRuneNodes;
    private List<Node> localRuneNodes;

    public RuneItemController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/RuneItem.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        newRuneNodes = Arrays.asList(shortName, source, importPage);
        localRuneNodes = Arrays.asList(name, selected, delete, edit);
    }

    public void setNewRuneMode(RunePage page) {
        newRuneNodes.forEach(node -> node.setVisible(true));
        localRuneNodes.forEach(node -> node.setVisible(false));
        shortName.setText(page.getName());
        try {
            source.setText("(" + new URI(page.getSource()).getHost() + ")");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        icon.setImage(SwingFXUtils.toFXImage(Objects.requireNonNull(page.getRunes()
                .get(0)
                .getImage()), null));
    }

    public void setLocalRuneMode(RunePage page) {
        setHomeRuneMode(page);
        edit.setVisible(false);
    }

    public void setHomeRuneMode(RunePage page) {
        newRuneNodes.forEach(node -> node.setVisible(false));
        localRuneNodes.forEach(node -> node.setVisible(true));
        name.setText(page.getName());
        icon.setImage(SwingFXUtils.toFXImage(Objects.requireNonNull(page.getRunes()
                .get(0)
                .getImage()), null));
        selected.setSelected(page.isFromClient());
    }

    public void onBuildsClick(ActionEvent actionEvent) {

    }

    public void onDelete(MouseEvent mouseEvent) {

    }

    public void onEdit(MouseEvent mouseEvent) {

    }

    public void onSelectChange(ActionEvent actionEvent) {

    }

    public void onPageImport(MouseEvent mouseEvent) {

    }

    public static class RunePageCell extends ListCell<RunePage> {

        private final BiConsumer<RuneItemController, RunePage> prepareFunction;
        private RuneItemController item;

        public RunePageCell(BiConsumer<RuneItemController, RunePage> prepareFunction) {
            this.prepareFunction = prepareFunction;
        }

        @Override
        public void updateItem(RunePage page, boolean empty) {
            super.updateItem(page, empty);
            if (item == null) {
                item = new RuneItemController();
            }
            if (page != null) {
                prepareFunction.accept(item, page);
                item.container.setVisible(true);
                setGraphic(item.container);
            }
            else {
                item.container.setVisible(false);
            }
        }

    }
}
