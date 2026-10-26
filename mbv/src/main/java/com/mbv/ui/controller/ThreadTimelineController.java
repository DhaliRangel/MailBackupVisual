package com.mbv.ui.controller;

import com.mbv.model.MailMessage;
import com.mbv.model.MailThread;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ThreadTimelineController {

    @FXML private VBox timelineContainer;
    @FXML private Label threadSubjectLabel;
    @FXML private Label threadCountLabel;

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
    }

    public void displayThread(MailThread thread) {
        if (thread == null) {
            clearDisplay();
            return;
        }

        threadSubjectLabel.setText(thread.getSubject());
        threadCountLabel.setText(thread.getMessageCount() + " messages in thread");

        timelineContainer.getChildren().clear();

        List<MailMessage> messages = thread.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            MailMessage msg = messages.get(i);
            boolean isLast = (i == messages.size() - 1);

            HBox node = createTimelineNode(msg, i, isLast);
            timelineContainer.getChildren().add(node);
        }
    }

    private HBox createTimelineNode(MailMessage msg, int index, boolean isLast) {
        HBox container = new HBox(10);
        container.getStyleClass().add("timeline-node");
        container.setPadding(new javafx.geometry.Insets(5));

        // Timeline line + dot
        VBox lineBox = new VBox(2);
        lineBox.setAlignment(javafx.geometry.Pos.CENTER);

        Circle dot = new Circle(6);
        dot.setFill(index == 0 ? Color.DODGERBLUE : Color.GRAY);
        lineBox.getChildren().add(dot);

        if (!isLast) {
            Label line = new Label("|");
            line.setTextFill(Color.GRAY);
            lineBox.getChildren().add(line);
        }

        // Message info
        VBox infoBox = new VBox(3);

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label senderLabel = new Label(msg.getSender() != null ? msg.getSender().getDisplayName() : "Unknown");
        senderLabel.getStyleClass().add("timeline-sender");

        Label dateLabel = new Label(msg.getSentDate() != null ? msg.getSentDate().format(DATE_FMT) : "");
        dateLabel.getStyleClass().add("timeline-date");

        headerRow.getChildren().addAll(senderLabel, dateLabel);

        String bodyPreview = "";
        if (msg.getPlainBody() != null && !msg.getPlainBody().isEmpty()) {
            bodyPreview = msg.getPlainBody().substring(0, Math.min(150, msg.getPlainBody().length()));
            bodyPreview = bodyPreview.replace("\n", " ").replace("\r", "");
        } else if (msg.getHtmlBody() != null) {
            bodyPreview = stripHtml(msg.getHtmlBody());
            bodyPreview = bodyPreview.substring(0, Math.min(150, bodyPreview.length()));
        }

        Label previewLabel = new Label(bodyPreview);
        previewLabel.getStyleClass().add("timeline-preview");
        previewLabel.setWrapText(true);

        infoBox.getChildren().addAll(headerRow, previewLabel);

        if (msg.hasAttachments()) {
            Label attLabel = new Label("\uD83D\uDCCE " + msg.getAttachmentCount() + " attachment(s)");
            attLabel.getStyleClass().add("timeline-attachment");
            infoBox.getChildren().add(attLabel);
        }

        container.getChildren().addAll(lineBox, infoBox);
        return container;
    }

    private void clearDisplay() {
        threadSubjectLabel.setText("");
        threadCountLabel.setText("");
        timelineContainer.getChildren().clear();
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .trim();
    }
}
