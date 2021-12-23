package com.stirante.runechanger.updater;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.UpdateContext;
import org.update4j.service.UpdateHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainController implements UpdateHandler {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    public ProgressBar overallProgress;

    @FXML
    public Label overallStatus;

    @FXML
    public ProgressBar currentProgress;

    @FXML
    public Label currentStatus;

    private List<FileMetadata> files = new ArrayList<>();
    private Configuration configuration;

    @FXML
    public void initialize() {
        overallStatus.setText("Initializing updater...");
        currentStatus.setText("");
        overallProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        currentProgress.setProgress(0);
    }

    private void doOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(runnable);
        }
    }

    @Override
    public void init(UpdateContext context) throws Throwable {
        configuration = context.getConfiguration();
    }

    @Override
    public void startCheckUpdates() throws Throwable {
        doOnFxThread(() -> {
            overallStatus.setText("Checking files for updates...");
            overallProgress.setProgress(0);
            currentProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        });
    }

    @Override
    public void startCheckUpdateFile(FileMetadata file) throws Throwable {
        doOnFxThread(() -> {
            currentStatus.setText("Checking file " + file.getPath() + "...");
        });
    }

    @Override
    public void doneCheckUpdateFile(FileMetadata file, boolean requires) throws Throwable {
        if (requires) {
            files.add(file);
        }
    }

    @Override
    public void updateCheckUpdatesProgress(float frac) throws Throwable {
        doOnFxThread(() -> {
            currentProgress.setProgress(frac);
        });
    }

    @Override
    public void startDownloads() throws Throwable {
        doOnFxThread(() -> {
            overallStatus.setText("Downloading files...");
            overallProgress.setProgress(0.1);
            currentProgress.setProgress(0);
        });
    }

    @Override
    public void startDownloadFile(FileMetadata file) throws Throwable {
        doOnFxThread(() -> {
            currentStatus.setText("Downloading file " + Path.of(System.getProperty("user.dir")).relativize(file.getPath()) + "...");
        });
    }

    @Override
    public void updateDownloadFileProgress(FileMetadata file, float frac) throws Throwable {
        doOnFxThread(() -> {
            currentProgress.setProgress(frac);
        });
    }

    @Override
    public void updateDownloadProgress(float frac) throws Throwable {
        doOnFxThread(() -> {
            overallProgress.setProgress(0.1 + (frac * 0.9));
        });
    }

    @Override
    public void doneDownloads() throws Throwable {
        doOnFxThread(() -> {
            overallStatus.setText("Files downloaded");
            currentStatus.setText("");
            overallProgress.setProgress(1);
            currentProgress.setProgress(1);
        });
    }

    @Override
    public void failed(Throwable t) {
        logger.error("Update failed", t);
        doOnFxThread(() -> {
            overallStatus.setText("Update failed!");
            currentStatus.setText("");
            overallProgress.setProgress(1);
            currentProgress.setProgress(1);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Update failed");
            alert.setHeaderText("Failed to update RuneChanger");
            alert.setContentText("We failed, but you can still update RuneChanger manually by downloading a zip file from GitHub. To preserve your rune pages and settings, keep all .dat files.");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label("The exception stacktrace was:");

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait();
            // Try to just open the old version
            try {
                Runtime.getRuntime().exec("wscript silent.vbs open.bat");
            } catch (IOException ignored) {
            }
            System.exit(0);
        });
    }

    @Override
    public void succeeded() {
        doOnFxThread(() -> {
            overallStatus.setText("RuneChanger updated!");
            currentStatus.setText("");
            overallProgress.setProgress(1);
            currentProgress.setProgress(1);
        });
        try {
            deleteOldLibs();
            Runtime.getRuntime().exec("wscript silent.vbs open.bat");
            System.exit(0);
        } catch (IOException e) {
            logger.error("Exception occurred while executing command", e);
        }
    }

    private void deleteOldLibs() {
        try {
            List<String> acceptableFiles = configuration.getFiles().stream()
                    .map(FileMetadata::getPath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            Files.walk(new File("lib").getAbsoluteFile().getParentFile().toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String relative = path.toString().replace(System.getProperty("user.dir"), "");
                        return relative.startsWith(File.separator + "image") ||
                                relative.startsWith(File.separator + "lib");
                    })
                    .filter(path -> acceptableFiles.stream()
                            .noneMatch(s -> path.toString().contains(s) || s.contains(path.toString())))
                    .map(Path::toFile)
                    .peek(file -> System.out.println("Deleting " + file))
                    .forEach(File::deleteOnExit);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
