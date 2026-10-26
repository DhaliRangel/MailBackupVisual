package com.mbv.ui.controller;

import com.mbv.model.SearchQuery;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FilterBarController {

    @FXML private DatePicker afterDatePicker;
    @FXML private DatePicker beforeDatePicker;
    @FXML private TextField senderFilter;
    @FXML private TextField recipientFilter;
    @FXML private TextField domainFilter;
    @FXML private CheckBox hasAttachmentFilter;
    @FXML private CheckBox htmlOnlyFilter;
    @FXML private CheckBox textOnlyFilter;
    @FXML private ComboBox<String> folderFilter;
    @FXML private Button applyFiltersBtn;
    @FXML private Button clearFiltersBtn;

    private MainController mainController;

    @FXML
    public void initialize() {
        folderFilter.getItems().addAll(
                "All", "Inbox", "Sent", "Drafts", "Trash", "Spam", "Archive"
        );
        folderFilter.setValue("All");

        applyFiltersBtn.setOnAction(e -> applyFilters());
        clearFiltersBtn.setOnAction(e -> clearFilters());
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    private void applyFilters() {
        SearchQuery query = buildQuery();
        // Delegate to main controller for search
        if (mainController != null) {
            // Apply filters to current search
            mainController.updateStatus("Filters applied");
        }
    }

    private void clearFilters() {
        afterDatePicker.setValue(null);
        beforeDatePicker.setValue(null);
        senderFilter.clear();
        recipientFilter.clear();
        domainFilter.clear();
        hasAttachmentFilter.setSelected(false);
        htmlOnlyFilter.setSelected(false);
        textOnlyFilter.setSelected(false);
        folderFilter.setValue("All");
    }

    public SearchQuery buildQuery() {
        SearchQuery query = new SearchQuery();

        if (senderFilter != null && !senderFilter.getText().isEmpty()) {
            query.setFrom(senderFilter.getText().trim());
        }
        if (recipientFilter != null && !recipientFilter.getText().isEmpty()) {
            query.setTo(recipientFilter.getText().trim());
        }
        if (domainFilter != null && !domainFilter.getText().isEmpty()) {
            query.setDomain(domainFilter.getText().trim());
        }
        if (afterDatePicker != null && afterDatePicker.getValue() != null) {
            query.setAfterDate(afterDatePicker.getValue());
        }
        if (beforeDatePicker != null && beforeDatePicker.getValue() != null) {
            query.setBeforeDate(beforeDatePicker.getValue());
        }
        if (hasAttachmentFilter != null && hasAttachmentFilter.isSelected()) {
            query.setHasAttachment(true);
        }
        if (htmlOnlyFilter != null && htmlOnlyFilter.isSelected()) {
            query.setOnlyHtml(true);
        }
        if (textOnlyFilter != null && textOnlyFilter.isSelected()) {
            query.setOnlyPlainText(true);
        }

        return query;
    }
}
