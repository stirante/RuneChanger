package com.stirante.runechanger.gui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog {
    private final Stage dialogStage;

    public ProgressDialog(String title) {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);

        ProgressBar pb = new ProgressBar();
        pb.setProgress(-1F);

        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().add(pb);

        Scene scene = new Scene(hb, 200, 100);
        dialogStage.setScene(scene);
    }

    public Stage getDialogStage() {
        return dialogStage;
    }
}

