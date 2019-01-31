/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.stirante.RuneChanger.gui;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXToggleButton;
import static com.stirante.RuneChanger.gui.Settings.craftKeys;
import static com.stirante.RuneChanger.gui.Settings.disenchantChampions;
import static com.stirante.RuneChanger.gui.Settings.mainStage;
import com.stirante.RuneChanger.util.SimplePreferences;
import java.util.Optional;
import javafx.animation.RotateTransition;
import javafx.animation.RotateTransitionBuilder;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SettingsController {

	@FXML
	private JFXButton disenchantBtn, craftKeyBtn;

	@FXML
	private ImageView btn_settings, btn_exit, btn_credits, btn_runebook, syncButton;

	@FXML
	private AnchorPane topbar_pane, settings_pane, credits_pane, runebook_pane;

	@FXML
	private JFXToggleButton quickReplyBtn, autoQueueBtn, noAwayBtn;

	@FXML
	private JFXComboBox<?> autoChampCB;

	@FXML
	void handleMenuSelection(MouseEvent event) {
		System.out.println("Menu item pressed" + event.getTarget());
		handleMenuSelectionFnc(event);
	}

	@FXML
	void handleSettingsButtonPressed(Event e) {
		System.out.println("Settings button pressed " + e);
		handleSettingsButtonSelection(e);
	}

	@FXML
	void handleToggleButtonPressed(Event e) {
		System.out.println("Toggle button pressed ");
		handleToggleButtonSelection(e);
	}

	@FXML
	void handleComboBoxPressed(Event e) {
		System.out.println("Combo box pressed" + e.getTarget());
		handleComboBox(e);
	}

	@FXML
	void handleSyncBtn(Event e) {
		System.out.println("Sync button pressed" + e.getTarget());
		handleRuneBookSyncButton(e);
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert topbar_pane != null : "fx:id=\"topbar_pane\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btn_settings != null : "fx:id=\"btn_settings\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btn_credits != null : "fx:id=\"btn_credits\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btn_runebook != null : "fx:id=\"btn_runebook\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btn_exit != null : "fx:id=\"btn_exit\" was not injected: check your FXML file 'Settings.fxml'.";
		assert settings_pane != null : "fx:id=\"settings_pane\" was not injected: check your FXML file 'Settings.fxml'.";
		assert credits_pane != null : "fx:id=\"credits_pane\" was not injected: check your FXML file 'Settings.fxml'.";
		SimplePreferences.load();
		loadPreferences();
	}

	private void handleMenuSelectionFnc(MouseEvent event)
	{
		if(event.getTarget() == btn_settings){
			settings_pane.setVisible(true);
			runebook_pane.setVisible(false);
			credits_pane.setVisible(false);

		}
		else if(event.getTarget() == btn_credits)
		{
			settings_pane.setVisible(false);
			runebook_pane.setVisible(false);
			credits_pane.setVisible(true);
		}
		else if (event.getTarget() == btn_runebook)
		{
			runebook_pane.setVisible(true);
			credits_pane.setVisible(false);
			settings_pane.setVisible(false);
		}
		else if(event.getTarget() == btn_exit)
		{
			settings_pane.setVisible(false);
			runebook_pane.setVisible(false);
			credits_pane.setVisible(false);
			mainStage.hide();
		}
	}

	private void handleSettingsButtonSelection(Event e)
	{
		if (e.getTarget() == craftKeyBtn)
		{
			craftKeys();
		}
		else if (e.getTarget() == disenchantBtn)
		{
			disenchantChampions();
		}
	}

	private void handleToggleButtonSelection (Event e)
	{
		if (e.getTarget() == autoChampCB)
		{
//			SimplePreferences.putValue("autoChamp", String.valueOf(autoChampCB-TODO));
		}
		else if (e.getTarget() == autoQueueBtn)
		{
			SimplePreferences.putValue("autoAccept", String.valueOf(autoQueueBtn.isSelected()));
		}
		else if (e.getTarget() == noAwayBtn)
		{
			SimplePreferences.putValue("antiAway", String.valueOf(noAwayBtn.isSelected()));
		}
		else if (e.getTarget() == quickReplyBtn)
		{
			SimplePreferences.putValue("quickReplies", String.valueOf(quickReplyBtn.isSelected()));
		}
		SimplePreferences.save();
	}

	private void loadPreferences ()
	{
		System.out.println("loading preferences..");
		if (!SimplePreferences.getValue("autoChamp").equals("None"))
		{
//			autoChampBtn.setSelected(TODO);
		}
		if (SimplePreferences.getValue("autoAccept").equals("true"))
		{
			autoQueueBtn.setSelected(true);
		}
		if (SimplePreferences.getValue("antiAway").equals("true"))
		{
			noAwayBtn.setSelected(true);
		}
		if (SimplePreferences.getValue("quickReplies").equals("true"))
		{
			quickReplyBtn.setSelected(true);
		}
	}

	private void handleComboBox(Event e)
	{

	}

	private void handleRuneBookSyncButton(Event e)
	{
		//TODO: Save the runebook here
		//////////////////////////////

		//////////////////////////////
		if (syncButton.isDisabled()) return;
		syncButton.setDisable(true);
		RotateTransition rt = new RotateTransition(Duration.millis(4500), syncButton);
		rt.setByAngle(1080);
      	rt.setCycleCount(1);
      	rt.play();
      	rt.setOnFinished(event -> {
			syncButton.setDisable(false);
		});
	}

	public static void showWarning (String title, String header, String content)
	{
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
