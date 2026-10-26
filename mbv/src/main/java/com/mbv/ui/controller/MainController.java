package com.mbv.ui.controller;

import com.mbv.config.ThemeManager;
import com.mbv.MbvApplication;
import com.mbv.model.*;
import com.mbv.service.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Component
public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private ToolBar toolbar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> themeSelector;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private SplitPane mainSplitPane;

    @FXML private FilterBarController filterBarController;
    @FXML private EmailListController emailListController;
    @FXML private EmailViewerController emailViewerController;

    @Autowired
    private ImportService importService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private ThemeManager themeManager;

    @Autowired
    private CacheService cacheService;

    private List<MailMessage> currentMessages = new ArrayList<>();

    @FXML
    public void initialize() {
        setupThemeSelector();
        setupSearchField();
        loadTheme();

        progressBar.setVisible(false);
        statusLabel.setText("Ready - No archive loaded");

        emailListController.setMainController(this);

        cacheService.initialize();
    }

    private void setupThemeSelector() {
        themeSelector.getItems().addAll(
                "Dark Blue", "Dark Green", "Dark Orange"
        );
        themeSelector.setValue("Dark Blue");
        themeSelector.setOnAction(e -> {
            String theme = themeSelector.getValue().toLowerCase().replace(" ", "-");
            themeManager.applyTheme(theme);
            cacheService.setTheme(theme);
        });
    }

    private void setupSearchField() {
        searchField.setOnAction(e -> performSearch());
    }

    private void loadTheme() {
        String savedTheme = cacheService.getTheme();
        if (savedTheme != null) {
            themeSelector.setValue(capitalizeTheme(savedTheme));
            themeManager.applyTheme(savedTheme);
        }
    }

    private String capitalizeTheme(String theme) {
        String[] parts = theme.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    @FXML
    private void onImportFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Email Archive Files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Email Archives", "*.mbox", "*.mbx", "*.eml"),
                new FileChooser.ExtensionFilter("Compressed Archives", "*.zip", "*.7z"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> files = chooser.showOpenMultipleDialog(MbvApplication.getPrimaryStage());
        if (files == null || files.isEmpty()) return;

        List<Path> paths = files.stream().map(File::toPath).toList();
        importFilesAsync(paths);
    }

    @FXML
    private void onImportFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Import");
        File dir = dirChooser.showDialog(MbvApplication.getPrimaryStage());

        if (dir == null) return;

        importFilesAsync(List.of(dir.toPath()));
    }

    private void importFilesAsync(List<Path> paths) {
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // indeterminate
        statusLabel.setText("Importing...");

        Task<List<MailMessage>> task = new Task<>() {
            @Override
            protected List<MailMessage> call() throws Exception {
                return importService.importFiles(paths, new ImportService.ImportProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String currentFile) {
                        Platform.runLater(() -> {
                            if (total > 0) {
                                progressBar.setProgress((double) current / total);
                            }
                            statusLabel.setText(currentFile);
                        });
                    }

                    @Override
                    public void onFileImported(String fileName, int messageCount) {
                        Platform.runLater(() ->
                                statusLabel.setText("Imported: " + fileName + " (" + messageCount + " messages)"));
                    }

                    @Override
                    public void onComplete(int totalMessages) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Import complete: " + totalMessages + " messages indexed");
                            progressBar.setVisible(false);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Error: " + error);
                            progressBar.setVisible(false);
                        });
                    }
                });
            }
        };

        task.setOnSucceeded(e -> {
            currentMessages = task.getValue();
            emailListController.setEmails(currentMessages);
            statusLabel.setText(currentMessages.size() + " messages loaded");
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Import failed: " + task.getException().getMessage());
            progressBar.setVisible(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            emailListController.setEmails(currentMessages);
            return;
        }

        try {
            List<SearchResult> results = searchService.parseAndSearch(query, 1000);
            List<MailMessage> messages = results.stream()
                    .map(SearchResult::getMessage)
                    .toList();
            emailListController.setEmails(messages);
            statusLabel.setText(results.size() + " results found");
        } catch (Exception e) {
            statusLabel.setText("Search error: " + e.getMessage());
        }
    }

    @FXML
    private void onExportCsv() {
        exportToFile("csv");
    }

    @FXML
    private void onExportEml() {
        exportToFolder("eml");
    }

    @FXML
    private void onExportPdf() {
        exportToFile("pdf");
    }

    private void exportToFile(String format) {
        List<MailMessage> toExport = emailListController.getSelectedEmails();
        if (toExport.isEmpty()) {
            toExport = currentMessages;
        }

        if (toExport.isEmpty()) {
            statusLabel.setText("No messages to export");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as " + format.toUpperCase());
        chooser.setInitialFileName("export." + format);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(format.toUpperCase(), "*." + format));

        File file = chooser.showSaveDialog(MbvApplication.getPrimaryStage());
        if (file == null) return;

        try {
            ExportService exportService = new ExportService();
            switch (format) {
                case "csv" -> exportService.exportToCsv(toExport, file.toPath());
                case "pdf" -> exportService.exportToPdf(toExport, file.toPath());
            }
            statusLabel.setText("Exported " + toExport.size() + " messages to " + file.getName());
        } catch (Exception e) {
            statusLabel.setText("Export error: " + e.getMessage());
        }
    }

    private void exportToFolder(String format) {
        List<MailMessage> toExport = emailListController.getSelectedEmails();
        if (toExport.isEmpty()) {
            toExport = currentMessages;
        }

        if (toExport.isEmpty()) {
            statusLabel.setText("No messages to export");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Export Directory");
        chooser.setInitialFileName("export");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EML Files", "*.eml"));
        File dir = chooser.showSaveDialog(MbvApplication.getPrimaryStage());
        if (dir == null) return;

        try {
            ExportService exportService = new ExportService();
            exportService.exportToEml(toExport, dir.getParentFile().toPath());
            statusLabel.setText("Exported " + toExport.size() + " messages as EML");
        } catch (Exception e) {
            statusLabel.setText("Export error: " + e.getMessage());
        }
    }

    public void onEmailSelected(MailMessage message) {
        emailViewerController.displayMessage(message);
    }

    public void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public List<MailMessage> getCurrentMessages() {
        return currentMessages;
    }
}
