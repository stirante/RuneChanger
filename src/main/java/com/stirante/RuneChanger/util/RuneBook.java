package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXListView;
import static com.stirante.RuneChanger.gui.SettingsController.rotateSyncButton;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import com.stirante.lolclient.ClientApi;
import generated.LolPerksPerkPageResource;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;

public class RuneBook
{
	private static ClientApi api = new ClientApi();
	private static ArrayList<LolPerksPerkPageResource> availablePages = new ArrayList<>();

	public static void importAction(JFXListView<Label> runebookList, ImageView syncButton)
	{
		availablePages.clear();
		ArrayList<LolPerksPerkPageResource> allAvailablePages = new ArrayList();
		rotateSyncButton(syncButton);
		allAvailablePages = getAvailablePages();
		LolPerksPerkPageResource currentRunes = new LolPerksPerkPageResource();
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.getButtonTypes().clear();
		alert.setTitle("Not that nice looking default windows ui");
		alert.setHeaderText("Choose which runepage to import.");
		System.out.println("Amount of rune pages: " + availablePages.size());
		for (int i = 0; i < availablePages.size(); i++)
		{
			alert.getButtonTypes().add(new ButtonType(availablePages.get(i).name));
		}
		alert.getButtonTypes().add(new ButtonType("Close"));
		Optional<ButtonType> option = alert.showAndWait();
		if (option.get() == null)
		{
			return;
		}
		for (int i = 0; i < availablePages.size(); i++)
		{
			if (option.get().getText() == availablePages.get(i).name)
			{
				currentRunes = availablePages.get(i);
			}
			else if (option.get().getText() == "Close")
			{
				alert.close();
				return;
			}
		}
		if (!currentRunes.isValid)
		{
			showWarning("Invalid Runes","The runes you chose are invalid","Please adjust your runes and try again.");
			return;
		}
		if (SimplePreferences.runeBookValues.containsKey(currentRunes.name))
		{
			showWarning("Duplicate runepage!", "The runepage you are trying to import already exists!", "Please remove or rename the runepage that has the duplicate name before trying again.");
			return;
		}
		System.out.println(currentRunes.selectedPerkIds);
		try
		{
			BufferedImage image = ImageIO.read(RuneBook.class.getResourceAsStream("/runes/" + currentRunes.selectedPerkIds.get(0) + ".png"));
			Label label = new Label(currentRunes.name.toUpperCase());
			ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
			imageView.setFitHeight(34);
			imageView.setFitWidth(34);
			label.autosize();
			label.setGraphic(imageView);
			runebookList.getItems().add(label);
			SimplePreferences.putRuneBookValue(currentRunes.name.toUpperCase(), currentRunes.selectedPerkIds.toString());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public static void updateListView(JFXListView<Label> runebookList)
	{

		SimplePreferences.runeBookValues.forEach((k, v) -> {
			try
			{
				String[] array = v.split(",");
				BufferedImage image = ImageIO.read(RuneBook.class.getResourceAsStream("/runes/" + array[0].replaceAll("\\[", "") + ".png"));
				Label label = new Label(k.toUpperCase());
				ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
				imageView.setFitHeight(34);
				imageView.setFitWidth(34);
				label.autosize();
				label.setGraphic(imageView);
				runebookList.getItems().add(label);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		});
	}

	public static void deleteRuneTree(JFXListView<Label> list)
	{
		try
		{
			Label selectedLabel = list.getFocusModel().getFocusedItem();
			list.getItems().remove(list.getFocusModel().getFocusedIndex());
			SimplePreferences.removeRuneBookValue(selectedLabel.getText().toUpperCase());
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Invalid selection user is trying to delete!");
		}
	}

	private static ArrayList<LolPerksPerkPageResource> getAvailablePages()
	{
		try
		{
			//get all rune pages
			LolPerksPerkPageResource[] pages = new LolPerksPerkPageResource[0];
			pages = api.executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
			//find available pages
			for (LolPerksPerkPageResource p : pages)
			{
				if (p.isEditable)
				{
					availablePages.add(p);
				}
			}
			if (availablePages.isEmpty())
			{
				System.out.println("No pages found, weird.");
				return null;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return availablePages;
	}

	private static LolPerksPerkPageResource getSelectedPage()
	{
		LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
		try
		{
			page1 = api.executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return page1;
	}
}