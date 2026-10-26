package com.mbv.ui.controller;

import com.mbv.model.MailMessage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class EmailViewerController {

    @FXML private TabPane viewerTabPane;
    @FXML private Tab bodyTab;
    @FXML private Tab headerTab;
    @FXML private Tab attachmentsTab;
    @FXML private Tab rawTab;

    @FXML private WebView htmlWebView;
    @FXML private TextArea plainBodyArea;
    @FXML private TextArea headerArea;
    @FXML private TextArea rawArea;
    @FXML private ListView<String> attachmentsList;

    @FXML private Label subjectLabel;
    @FXML private Label fromLabel;
    @FXML private Label toLabel;
    @FXML private Label dateLabel;
    @FXML private Label folderLabel;

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        plainBodyArea.setEditable(false);
        headerArea.setEditable(false);
        rawArea.setEditable(false);
    }

    public void displayMessage(MailMessage message) {
        if (message == null) {
            clearDisplay();
            return;
        }

        subjectLabel.setText(message.getSubject() != null ? message.getSubject() : "(No Subject)");
        fromLabel.setText(message.getSender() != null ? message.getSender().getDisplayName() : "Unknown");
        toLabel.setText(formatRecipients(message));
        dateLabel.setText(message.getSentDate() != null ? message.getSentDate().format(DATE_FMT) : "Unknown date");
        folderLabel.setText(message.getFolder() != null ? message.getFolder() : "");

        // Body
        if (message.getHtmlBody() != null && !message.getHtmlBody().isEmpty()) {
            htmlWebView.getEngine().loadContent(wrapWithDarkTheme(message.getHtmlBody()));
            plainBodyArea.setText(stripHtml(message.getHtmlBody()));
            viewerTabPane.getSelectionModel().select(bodyTab);
        } else if (message.getPlainBody() != null && !message.getPlainBody().isEmpty()) {
            plainBodyArea.setText(message.getPlainBody());
            htmlWebView.getEngine().loadContent(wrapWithDarkTheme("<pre style='color:#c9d1d9;font-family:monospace;'>" + message.getPlainBody() + "</pre>"));
        } else {
            plainBodyArea.setText("(No body content)");
            htmlWebView.getEngine().loadContent(wrapWithDarkTheme("<p style='color:#484f58;'>No body content</p>"));
        }

        // Headers
        StringBuilder headers = new StringBuilder();
        headers.append("Message-ID: ").append(message.getMessageId()).append("\n");
        headers.append("From: ").append(message.getSender() != null ? message.getSender().getDisplayName() : "").append("\n");
        headers.append("To: ").append(formatRecipients(message)).append("\n");
        if (!message.getCc().isEmpty()) {
            headers.append("Cc: ").append(message.getCc().stream()
                    .map(c -> c.getDisplayName()).collect(Collectors.joining(", "))).append("\n");
        }
        headers.append("Subject: ").append(message.getSubject()).append("\n");
        headers.append("Date: ").append(message.getSentDate() != null ? message.getSentDate().format(DATE_FMT) : "").append("\n");
        if (message.getInReplyTo() != null) {
            headers.append("In-Reply-To: ").append(message.getInReplyTo()).append("\n");
        }
        if (!message.getReferences().isEmpty()) {
            headers.append("References: ").append(String.join(" ", message.getReferences())).append("\n");
        }
        headers.append("MIME Type: ").append(message.getMimeType()).append("\n");
        headers.append("Charset: ").append(message.getCharset()).append("\n");
        headers.append("Size: ").append(message.getSize()).append(" bytes\n");
        headers.append("Archive: ").append(message.getSourceArchive()).append("\n");
        headers.append("Format: ").append(message.getSourceArchiveType()).append("\n");
        headerArea.setText(headers.toString());

        // Attachments
        attachmentsList.getItems().clear();
        if (message.hasAttachments()) {
            message.getAttachments().forEach(att ->
                    attachmentsList.getItems().add(att.toString()));
        } else {
            attachmentsList.getItems().add("(No attachments)");
        }

        // Raw
        rawArea.setText(buildRawMessage(message));
    }

    private void clearDisplay() {
        subjectLabel.setText("");
        fromLabel.setText("");
        toLabel.setText("");
        dateLabel.setText("");
        folderLabel.setText("");
        plainBodyArea.clear();
        headerArea.clear();
        rawArea.clear();
        attachmentsList.getItems().clear();
        htmlWebView.getEngine().loadContent("");
    }

    private String formatRecipients(MailMessage message) {
        return message.getTo().stream()
                .map(c -> c.getDisplayName())
                .collect(Collectors.joining(", "));
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .trim();
    }

    private String buildRawMessage(MailMessage message) {
        StringBuilder raw = new StringBuilder();
        raw.append("From: ").append(message.getSender() != null ? message.getSender().getDisplayName() : "").append("\r\n");
        raw.append("To: ").append(formatRecipients(message)).append("\r\n");
        raw.append("Subject: ").append(message.getSubject()).append("\r\n");
        raw.append("Date: ").append(message.getSentDate()).append("\r\n");
        raw.append("Message-ID: <").append(message.getMessageId()).append(">\r\n");
        raw.append("\r\n");

        if (message.getPlainBody() != null) {
            raw.append(message.getPlainBody());
        } else if (message.getHtmlBody() != null) {
            raw.append(message.getHtmlBody());
        }

        return raw.toString();
    }

    private String wrapWithDarkTheme(String htmlBody) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <style>
                body {
                    background-color: #0d1117;
                    color: #c9d1d9;
                    font-family: 'Segoe UI', system-ui, sans-serif;
                    font-size: 13px;
                    margin: 0;
                    padding: 12px;
                    line-height: 1.5;
                }
                a { color: #58a6ff; }
                img { max-width: 100%%; }
                pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; }
                table { border-collapse: collapse; }
                td, th { padding: 4px 8px; }
            </style>
            </head>
            <body>%s</body>
            </html>
            """.formatted(htmlBody);
    }
}
