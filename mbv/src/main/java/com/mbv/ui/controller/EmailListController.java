package com.mbv.ui.controller;

import com.mbv.model.MailMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmailListController {

    @FXML private ListView<MailMessage> emailListView;
    @FXML private Label countLabel;

    private ObservableList<MailMessage> emailList = FXCollections.observableArrayList();
    private MainController mainController;
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        emailListView.setItems(emailList);
        emailListView.setCellFactory(list -> new EmailListCell());

        emailListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && mainController != null) {
                        mainController.onEmailSelected(newVal);
                    }
                }
        );
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public void setEmails(List<MailMessage> messages) {
        emailList.setAll(messages);
        if (countLabel != null) {
            countLabel.setText(messages.size() + " messages");
        }
    }

    public List<MailMessage> getSelectedEmails() {
        return new ArrayList<>(emailListView.getSelectionModel().getSelectedItems());
    }

    public MailMessage getSelectedEmail() {
        return emailListView.getSelectionModel().getSelectedItem();
    }

    private class EmailListCell extends ListCell<MailMessage> {
        @Override
        protected void updateItem(MailMessage item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            HBox container = new HBox(8);
            container.getStyleClass().add("email-list-item");

            // Unread indicator
            if (item.getFlags() != null && !item.getFlags().isRead()) {
                Label unreadDot = new Label("\u25CF");
                unreadDot.getStyleClass().add("unread-indicator");
                container.getChildren().add(unreadDot);
            }

            // Email content box
            VBox contentBox = new VBox(2);

            // Subject + Date row
            HBox headerRow = new HBox(8);
            headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label subjectLabel = new Label(item.getSubject() != null ? item.getSubject() : "(No Subject)");
            subjectLabel.getStyleClass().add("email-subject");
            subjectLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(subjectLabel, javafx.scene.layout.Priority.ALWAYS);

            Label dateLabel = new Label(item.getSentDate() != null ? item.getSentDate().format(DATE_FMT) : "");
            dateLabel.getStyleClass().add("email-date");

            headerRow.getChildren().addAll(subjectLabel, dateLabel);

            // From line
            String fromText = item.getSender() != null ? item.getSender().getDisplayName() : "Unknown";
            Label fromLabel = new Label(fromText);
            fromLabel.getStyleClass().add("email-from");

            // Preview line
            String preview = "";
            if (item.getPlainBody() != null && !item.getPlainBody().isEmpty()) {
                preview = item.getPlainBody().substring(0, Math.min(100, item.getPlainBody().length()));
                preview = preview.replace("\n", " ").replace("\r", "");
            }
            Label previewLabel = new Label(preview);
            previewLabel.getStyleClass().add("email-preview");

            contentBox.getChildren().addAll(headerRow, fromLabel, previewLabel);
            container.getChildren().add(contentBox);

            // Attachment indicator
            if (item.hasAttachments()) {
                Label attIcon = new Label("\uD83D\uDCCE " + item.getAttachmentCount());
                attIcon.getStyleClass().add("attachment-indicator");
                container.getChildren().add(attIcon);
            }

            setGraphic(container);
            setText(null);
        }
    }
}
