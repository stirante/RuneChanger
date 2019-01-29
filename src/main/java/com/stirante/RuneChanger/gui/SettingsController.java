/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.stirante.RuneChanger.gui;

import static com.stirante.RuneChanger.gui.Settings.main;
import static com.stirante.RuneChanger.gui.Settings.mainStage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class SettingsController {

	@FXML
	private ImageView btn_settings, btn_exit, btn_credits, btn_runebook;

	@FXML
	private AnchorPane topbar_pane, settings_pane, credits_pane;

	@FXML
	void handleButtonAction(MouseEvent event) {
		System.out.println(event.getTarget());
		handleMenuSelection(event);
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

	}

	private void handleMenuSelection(MouseEvent event)
	{
		if(event.getTarget() == btn_settings){
			settings_pane.setVisible(true);
			credits_pane.setVisible(false);

		}
		else
		if(event.getTarget() == btn_credits)
		{
			settings_pane.setVisible(false);
			credits_pane.setVisible(true);
		}
		else
		if(event.getTarget() == btn_exit)
		{
			settings_pane.setVisible(false);
			credits_pane.setVisible(false);
			mainStage.hide();
//			System.exit(0);
		}
	}
}
