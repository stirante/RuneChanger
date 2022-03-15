package com.stirante.runechanger.gui.controllers;

import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.ChampionBuild;
import com.stirante.runechanger.api.RuneBook;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.gui.Settings;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class RuneItemController {
    private static final Logger log = LoggerFactory.getLogger(RuneItemController.class);
    private final RuneChangerApi api;

    public Pane container;
    public ImageView icon;
    public Label name;
    public CheckBox selected;
    public ImageView delete;
    public ImageView edit;
    public Label shortName;
    public Label source;
    public ImageView importPage;
    public Circle syncStatus;
    private List<Node> newRuneNodes;
    private List<Node> localRuneNodes;
    private ChampionBuild page;

    public RuneItemController(RuneChangerApi api) {
        this.api = api;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/RuneItem.fxml"), RuneChanger.getInstance()
                .getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        newRuneNodes = Arrays.asList(shortName, source, importPage);
        localRuneNodes = Arrays.asList(name, selected, delete);
        edit.setVisible(false);
        Tooltip statusTooltip = new Tooltip(RuneChanger.getInstance().getLang().getString("runepage_not_synced"));
        statusTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(syncStatus, statusTooltip);
        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.ZERO);
        selected.setTooltip(tooltip);
    }

    public void setNewRuneMode(ChampionBuild page) {
        this.page = page;
        newRuneNodes.forEach(node -> node.setVisible(true));
        localRuneNodes.forEach(node -> node.setVisible(false));
        shortName.setText(page.getName());
        try {
            source.setText("(" + new URI(page.getSource()).getHost() + ")");
        } catch (URISyntaxException e) {
            log.error("Exception occurred while getting source host", e);
        }
        icon.setImage(SwingFXUtils.toFXImage(Objects.requireNonNull(page.getRunePage().getRunes()
                .get(0)
                .getImage()), null));
    }

    public void setLocalRuneMode(ChampionBuild page) {
        setHomeRuneMode(page);
    }

    public void setHomeRuneMode(ChampionBuild page) {
        this.page = page;
        newRuneNodes.forEach(node -> node.setVisible(false));
        localRuneNodes.forEach(node -> node.setVisible(true));
        name.setText(page.getName());
        icon.setImage(SwingFXUtils.toFXImage(Objects.requireNonNull(page.getRunePage().getRunes()
                .get(0)
                .getImage()), null));
        selected.setSelected(page.getRunePage().isFromClient());
        syncStatus.setVisible(page.getRunePage().isFromClient() && !page.getRunePage().isSynced());
        selected.getTooltip()
                .setText(page.getRunePage().isFromClient() ?
                        RuneChanger.getInstance().getLang().getString("client_runepage") :
                        RuneChanger.getInstance().getLang().getString("local_runepage"));
    }

    @FXML
    public void onBuildsClick(ActionEvent actionEvent) {
        //TODO
    }

    @FXML
    public void onDelete(MouseEvent mouseEvent) {
        boolean removePage = Settings.openYesNoDialog(
                RuneChanger.getInstance().getLang().getString("remove_page_confirmation_title"),
                String.format(RuneChanger.getInstance().getLang().getString("remove_page_confirmation_message"), page.getName()));
        if (removePage) {
            if (page.getRunePage().isFromClient()) {
                RuneChanger.getInstance().getRunesModule().deletePage(page.getRunePage());
            }
            if (api.getRuneBook().getRuneBookPage(page.getName()) == null) {
                return;
            }
            api.getRuneBook().removeRuneBookPage(page.getName());
        }
    }

    @FXML
    public void onEdit(MouseEvent mouseEvent) {
        //TODO
    }

    @FXML
    public void onSelectChange(ActionEvent actionEvent) {
        // Don't do anything, if we're not connected to the client
        if (!api.getClientApi().isConnected()) {
            selected.setSelected(false);
            RuneChanger.getInstance()
                    .getGuiHandler()
                    .showWarningMessage(RuneChanger.getInstance().getLang().getString("not_connected_warning"));
            return;
        }
        // Page is from client and we're deselecting it, so we need to remove it
        if (page.getRunePage().isFromClient() && !selected.isSelected()) {
            RuneChanger.getInstance().getRunesModule().deletePage(page.getRunePage());
        }
        // Page is not selected, we're selecting it and we have at least one free rune page. We need to add it to client
        else if (selected.isSelected() && RuneChanger.getInstance().getRunesModule().getOwnedPageCount() >
                RuneChanger.getInstance().getRunesModule().getRunePages().size()) {
            RuneChanger.getInstance().getRunesModule().addPage(page.getRunePage());
        }
        selected.getTooltip()
                .setText(page.getRunePage().isFromClient() ?
                        RuneChanger.getInstance().getLang().getString("client_runepage") :
                        RuneChanger.getInstance().getLang().getString("local_runepage"));
    }

    @FXML
    public void onPageImport(MouseEvent mouseEvent) {
        if (api.getRuneBook().getRuneBookPage(page.getName()) != null) {
            RuneChanger.getInstance()
                    .getGuiHandler()
                    .showWarningMessage(RuneChanger.getInstance().getLang().getString("page_already_exists"));
            return;
        }
        api.getRuneBook().addRuneBookPage(page.getRunePage());
    }

    public static class ChampionBuildCell extends ListCell<ChampionBuild> {

        private final RuneChangerApi api;
        private final BiConsumer<RuneItemController, ChampionBuild> prepareFunction;
        private RuneItemController item;

        public ChampionBuildCell(RuneChangerApi api, BiConsumer<RuneItemController, ChampionBuild> prepareFunction) {
            this.api = api;
            this.prepareFunction = prepareFunction;
        }

        @Override
        public void updateItem(ChampionBuild page, boolean empty) {
            super.updateItem(page, empty);
            if (item == null) {
                item = new RuneItemController(api);
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
