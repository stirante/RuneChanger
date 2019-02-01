package com.stirante.RuneChanger.util;

import com.jfoenix.controls.JFXListView;
import static com.stirante.RuneChanger.gui.SettingsController.rotateSyncButton;
import static com.stirante.RuneChanger.gui.SettingsController.showWarning;
import com.stirante.lolclient.ClientApi;
import generated.LolPerksPerkPageResource;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	private static Integer primaryStyle = null;
	private static Integer subStyle = null;

	public static void importAction(JFXListView<Label> runebookList, ImageView syncButton)
	{
		availablePages.clear();
		rotateSyncButton(syncButton);
		getAvailablePages();
		LolPerksPerkPageResource currentRunes = new LolPerksPerkPageResource();
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.getButtonTypes().clear();
		alert.setTitle("Runepage Selection");
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
		if (currentRunes.isValid == null || !currentRunes.isValid)
		{
			showWarning("Invalid Runes", "The runes you chose are invalid or incomplete", "Please adjust your runes and try again.");
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
			SimplePreferences.putRuneBookValue(currentRunes.name.toUpperCase(), currentRunes.selectedPerkIds.toString() + "-" + currentRunes.primaryStyleId.toString() + "-" + currentRunes.subStyleId.toString());
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

	public static void loadAction(JFXListView<Label> list)
	{
		if (list.getFocusModel().getFocusedItem() == null)
		{
			System.out.println("No selection made to load");
			return;
		}
		primaryStyle = null;
		subStyle = null;
		try
		{
			LolPerksPerkPageResource page1;
			page1 = getSelectedPage();

			System.out.println("page1: " + page1.name + " " + page1.selectedPerkIds);

			if (!page1.isEditable)
			{
				showWarning("Page not editable!","The page you have chosen is not editable.","To continue you need to choose a editable page in the league client and try again.");
				return;
			}

			System.out.println(page1);
			page1.selectedPerkIds = processPerks(SimplePreferences.getRuneBookValue(list.getFocusModel().getFocusedItem().getText()));
			page1.name = list.getFocusModel().getFocusedItem().getText();
			page1.isActive = true;
			page1.primaryStyleId = primaryStyle;
			page1.subStyleId = subStyle;
			System.out.println("Page: " + page1);
			api.executeDelete("/lol-perks/v1/pages/" + page1.id);
			api.executePost("/lol-perks/v1/pages/", page1);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private static List<Integer> processPerks(String processThis)
	{
		List<Integer> list = new ArrayList<>();
		System.out.println("Stuff: " + processThis.toString());
		String[] runesAndStyles = processThis.split("-");
		String runes = runesAndStyles[0];
		String primaryStyleId = runesAndStyles[1];
		String subStyleId = runesAndStyles[2];
		System.out.println("Stuff: " + subStyleId + " " + primaryStyleId + " " + runes);
		String[] array = runes.split(",");
		array[0].replaceAll("\\[", "");
		array[array.length - 1].replaceAll("\\]", "");
		for (int i = 0; array.length > i; i++)
		{
			String parseThis = array[i].replaceAll("\\[","");
			parseThis = parseThis.replaceAll("]","");
			parseThis = parseThis.replaceAll("\\s","");
			list.add(Integer.parseInt(parseThis));
			System.out.println(array[i]);
		}
		primaryStyle = Integer.parseInt(primaryStyleId);
		subStyle = Integer.parseInt(subStyleId);
		System.out.println("List Processed: " + list + "\nPrimaryStyle processed: " + primaryStyleId + "\nSubStyle processed: " + subStyleId);
		return list;
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