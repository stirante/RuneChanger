package com.stirante.RuneChanger.gui.controllers;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.jfoenix.controls.JFXProgressBar;
import com.stirante.RuneChanger.gui.ControllerUtil;
import com.stirante.RuneChanger.model.LogRequest;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.PathUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class LogsController implements Initializable {

    @FXML
    private JFXProgressBar progressBar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    @FXML
    void clearLogs(ActionEvent event) {
        File file = PathUtils.getLogsDir();
        log.warn("Log clear event registered, preparing to remove all files in: " + file.toString());
        if (file.listFiles().length < 2) {
            ControllerUtil.getInstance().showInfo("Nothing to remove", "Your log directory is already empty!");
            return;
        }
        if (!ControllerUtil.getInstance().showConfirmationScreen("Log Deletion Confirmation", "Are you sure you want to delete " + (file.listFiles().length - 1) + " files?")) {
            return;
        }
        new Thread(() -> {
            try {
                deleteFile(file, progressBar);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void sendLogs(ActionEvent event) {
        log.info("Sending logs to remote server..");
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext(); ) {
                Appender<ILoggingEvent> appender = index.next();
                if (appender instanceof FileAppender) {
                    try {
                        byte[] encoded =
                                Files.readAllBytes(Paths.get(((FileAppender<ILoggingEvent>) appender).getFile()));
                        String code = new LogRequest(new String(encoded)).submit();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(LangHelper.getLang().getString("logs_sent"));
                        alert.setHeaderText(null);
                        alert.setContentText(
                                String.format(LangHelper.getLang().getString("logs_sent_msg"), code));
                        ButtonType btn = new ButtonType(LangHelper.getLang().getString("copy_code"));
                        alert.getButtonTypes().add(btn);
                        Optional<ButtonType> buttonType = alert.showAndWait();
                        if (buttonType.isPresent() && buttonType.get() == btn) {
                            StringSelection stringSelection = new StringSelection(code);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(stringSelection, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void deleteFile(File element, JFXProgressBar bar) throws InterruptedException {
        for (File sub : element.listFiles()) {
            sub.delete();
        }
        Thread.sleep(300);
        Platform.runLater(() -> ControllerUtil.getInstance()
                .showInfo("Finished clearing logs!", "Your log directory has succesfully been cleared!"));
    }
}
