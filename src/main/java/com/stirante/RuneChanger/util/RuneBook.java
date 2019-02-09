package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXListView;
import com.stirante.RuneChanger.RuneChanger;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.model.Style;
import com.stirante.RuneChanger.runestore.LocalSource;
import com.stirante.RuneChanger.runestore.RuneStore;
import com.stirante.lolclient.ClientApi;
import generated.LolPerksPerkPageResource;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

public class RuneBook {
    private static HashMap<String, RunePage> availablePages = new HashMap<>();

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

    public static void importLocalRunes(JFXListView<Label> localList, JFXListView<Label> clientList) {
        Label focusedItem = clientList.getFocusModel().getFocusedItem();
        if (focusedItem == null) {
            return;
        }
        if (!localList.getItems()
                .filtered(label -> label.getText().equalsIgnoreCase(focusedItem.getText()))
                .isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Duplicate name!");
            alert.setHeaderText(null);
            alert.setContentText("You already have rune page with name \"" + focusedItem.getText() + "\"!");
            alert.showAndWait();
            return;
        }
        RunePage runePage = availablePages.get(focusedItem.getText());
        if (runePage == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing page!");
            alert.setHeaderText(null);
            alert.setContentText("The rune page you are trying to import is not available!");
            alert.showAndWait();
            return;
        }
        SimplePreferences.addRuneBookPage(runePage);
        localList.getItems().add(createListElement(runePage));
    }

    private static Label createListElement(RunePage page) {
        try {
            BufferedImage image = ImageIO.read(RuneBook.class.getResourceAsStream(
                    "/runes/" + page.getRunes().get(0).getId() + ".png"));
            Label label = new Label(page.getName());
            label.setTextFill(Color.WHITE);
            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
            imageView.setFitHeight(25);
            imageView.setFitWidth(25);
            label.autosize();
            label.setGraphic(imageView);
            return label;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
		HashMap<String, Integer> map = new HashMap();
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

	public static void loadAction(JFXListView<Label> localRuneList, JFXListView<Label> clientRunesList)
	{
		ClientApi api = RuneChanger.getApi();
		refreshAvailablePages();
		LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
		if (localRuneList.getFocusModel().getFocusedItem() == null || clientRunesList.getFocusModel().getFocusedItem() == null)
		{
			showWarning("No runepage to load to or from selected!", "Please select a runepage to load to and from from both the lists.", null);
			return;
		}

		HashMap<String, Integer> sortedPageIds = getSortedPageIds();
		Integer id = sortedPageIds.get(clientRunesList.getFocusModel().getFocusedItem().getText());
		if (id == null)
		{
			showWarning("The runepage you are trying to overwrite does not exist.", "Please refresh your runepages and then continue.", null);
			return;
		}
		new Thread(() -> {
			try
			{
				List<RunePage> runePageList = RuneStore.getLocalRunepageByName(localRuneList.getFocusModel().getFocusedItem().getText());
				if (runePageList.isEmpty())
				{
					showWarning("No page by that name", "There is no locally stored runepage by the name: " + localRuneList.getFocusModel().getFocusedItem().getText(), null);
					return;
				}
				runePageList.forEach(runepage -> runepage.toClient(page1));

				api.executeDelete("/lol-perks/v1/pages/" + id);
				api.executePost("/lol-perks/v1/pages/", page1);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}).start();
	}

	private static LolPerksPerkPageResource getSelectedPage()
	{
		LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
		try
		{
			page1 = RuneChanger.getApi().executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return page1;
	}
}