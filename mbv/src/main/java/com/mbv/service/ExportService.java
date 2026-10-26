package com.mbv.service;

import com.mbv.model.MailMessage;
import com.mbv.model.MailAttachment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void exportToCsv(List<MailMessage> messages, Path outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("ID,Date,From,To,Subject,Attachments,Size,Folder,Source");
            writer.newLine();

            for (MailMessage msg : messages) {
                writer.write(String.join(",",
                        escapeCsv(msg.getId()),
                        escapeCsv(msg.getSentDate() != null ? msg.getSentDate().format(DATE_FMT) : ""),
                        escapeCsv(msg.getSender() != null ? msg.getSender().getEmail() : ""),
                        escapeCsv(formatRecipients(msg)),
                        escapeCsv(msg.getSubject()),
                        escapeCsv(String.valueOf(msg.getAttachmentCount())),
                        escapeCsv(String.valueOf(msg.getSize())),
                        escapeCsv(msg.getFolder()),
                        escapeCsv(msg.getSourceArchive())
                ));
                writer.newLine();
            }
        }
    }

    public void exportToEml(List<MailMessage> messages, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        for (MailMessage msg : messages) {
            String fileName = sanitizeFileName(msg.getId()) + ".eml";
            Path outputFile = outputDir.resolve(fileName);

            // Reconstruct basic RFC 822 format
            StringBuilder eml = new StringBuilder();
            eml.append("Message-ID: <").append(msg.getMessageId()).append(">\r\n");
            if (msg.getSender() != null) {
                eml.append("From: ").append(msg.getSender().getDisplayName()).append("\r\n");
            }
            for (var to : msg.getTo()) {
                eml.append("To: ").append(to.getDisplayName()).append("\r\n");
            }
            for (var cc : msg.getCc()) {
                eml.append("Cc: ").append(cc.getDisplayName()).append("\r\n");
            }
            eml.append("Subject: ").append(msg.getSubject()).append("\r\n");
            if (msg.getSentDate() != null) {
                eml.append("Date: ").append(msg.getSentDate().format(DATE_FMT)).append("\r\n");
            }
            if (msg.getInReplyTo() != null) {
                eml.append("In-Reply-To: <").append(msg.getInReplyTo()).append(">\r\n");
            }
            eml.append("\r\n");

            if (msg.getPlainBody() != null) {
                eml.append(msg.getPlainBody());
            } else if (msg.getHtmlBody() != null) {
                eml.append(msg.getHtmlBody());
            }

            Files.writeString(outputFile, eml.toString());
        }
    }

    public void exportToPdf(List<MailMessage> messages, Path outputFile) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, Files.newOutputStream(outputFile));
        document.open();

        document.add(new Paragraph("MBV - Mail Export"));
        document.add(new Paragraph("Exported: " + java.time.LocalDateTime.now().format(DATE_FMT)));
        document.add(new Paragraph("Total messages: " + messages.size()));
        document.add(new Paragraph(" "));

        for (MailMessage msg : messages) {
            document.add(new Paragraph("---"));

            String header = String.format("Date: %s | From: %s | Subject: %s",
                    msg.getSentDate() != null ? msg.getSentDate().format(DATE_FMT) : "N/A",
                    msg.getSender() != null ? msg.getSender().getEmail() : "N/A",
                    msg.getSubject() != null ? msg.getSubject() : "N/A");
            document.add(new Paragraph(header));

            if (msg.getTo() != null && !msg.getTo().isEmpty()) {
                document.add(new Paragraph("To: " + formatRecipients(msg)));
            }

            if (msg.getAttachmentCount() > 0) {
                document.add(new Paragraph("Attachments: " + msg.getAttachmentCount()));
            }

            document.add(new Paragraph(" "));

            String body = msg.getPlainBody() != null ? msg.getPlainBody() : "";
            if (body.length() > 5000) {
                body = body.substring(0, 5000) + "... [truncated]";
            }
            if (!body.isEmpty()) {
                document.add(new Paragraph(body));
            }

            document.add(new Paragraph(" "));
        }

        document.close();
    }

    public void exportToJson(List<MailMessage> messages, Path outputFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputFile.toFile(), messages);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatRecipients(MailMessage msg) {
        StringBuilder sb = new StringBuilder();
        for (var c : msg.getTo()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(c.getEmail());
        }
        return sb.toString();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
