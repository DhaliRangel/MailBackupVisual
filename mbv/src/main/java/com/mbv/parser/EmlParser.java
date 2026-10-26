package com.mbv.parser;

import com.mbv.model.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Component
public class EmlParser implements MailParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".eml");
    }

    @Override
    public Stream<MailMessage> parse(Path file) throws IOException {
        List<MailMessage> messages = new ArrayList<>();

        try {
            String rawContent = Files.readString(file);
            MailMessage msg = parseRawMessage(rawContent);
            if (msg != null) {
                msg.setSourceArchive(file.getParent() != null
                        ? file.getParent().getFileName().toString()
                        : "EML Import");
                msg.setSourceArchiveType("EML");
                msg.setOriginalPath(file.toString());
                messages.add(msg);
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse EML file: " + file, e);
        }

        return messages.stream();
    }

    private MailMessage parseRawMessage(String rawMessage) throws MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session,
                new ByteArrayInputStream(rawMessage.getBytes("UTF-8")));

        MailMessage msg = new MailMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setMessageId(extractMessageId(mimeMessage));
        msg.setInReplyTo(mimeMessage.getHeader("In-Reply-To", null));
        msg.setSubject(decodeHeader(mimeMessage.getSubject()));

        msg.setSender(extractContact(mimeMessage.getFrom()));
        msg.setTo(extractContacts(mimeMessage.getRecipients(Message.RecipientType.TO)));
        msg.setCc(extractContacts(mimeMessage.getRecipients(Message.RecipientType.CC)));
        msg.setBcc(extractContacts(mimeMessage.getRecipients(Message.RecipientType.BCC)));

        if (mimeMessage.getSentDate() != null) {
            msg.setSentDate(mimeMessage.getSentDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        extractBody(mimeMessage, msg);
        extractAttachments(mimeMessage, msg);
        extractFlags(mimeMessage, msg);
        extractPriority(mimeMessage, msg);
        extractReferences(mimeMessage, msg);

        msg.setSize(rawMessage.getBytes("UTF-8").length);
        msg.setHash(computeHash(rawMessage));
        msg.setFolder("Inbox");

        return msg;
    }

    private String extractMessageId(MimeMessage message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0].replaceAll("[<>]", "").trim();
            }
        } catch (MessagingException e) {
            // ignore
        }
        return UUID.randomUUID().toString();
    }

    private String decodeHeader(String header) {
        if (header == null) return "";
        try {
            return MimeUtility.decodeText(header);
        } catch (UnsupportedEncodingException e) {
            return header;
        }
    }

    private Contact extractContact(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return null;
        Address addr = addresses[0];
        if (addr instanceof InternetAddress) {
            InternetAddress iAddr = (InternetAddress) addr;
            return new Contact(decodeHeader(iAddr.getPersonal()), iAddr.getAddress());
        }
        return new Contact(addr.toString());
    }

    private List<Contact> extractContacts(Address[] addresses) {
        List<Contact> contacts = new ArrayList<>();
        if (addresses == null) return contacts;
        for (Address addr : addresses) {
            if (addr instanceof InternetAddress) {
                InternetAddress iAddr = (InternetAddress) addr;
                contacts.add(new Contact(decodeHeader(iAddr.getPersonal()), iAddr.getAddress()));
            } else {
                contacts.add(new Contact(addr.toString()));
            }
        }
        return contacts;
    }

    private void extractBody(MimeMessage message, MailMessage msg) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String) {
            String contentType = message.getContentType().toLowerCase();
            if (contentType.contains("text/html")) {
                msg.setHtmlBody((String) content);
            } else {
                msg.setPlainBody((String) content);
            }
        } else if (content instanceof Multipart) {
            extractMultipartBody((Multipart) content, msg);
        }
    }

    private void extractMultipartBody(Multipart multipart, MailMessage msg) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType().toLowerCase();

            if (bodyPart.getFileName() == null || bodyPart.getFileName().isEmpty()) {
                if (contentType.contains("text/html") && msg.getHtmlBody() == null) {
                    msg.setHtmlBody(readContent(bodyPart));
                } else if (contentType.contains("text/plain") && msg.getPlainBody() == null) {
                    msg.setPlainBody(readContent(bodyPart));
                }
            }

            if (bodyPart.getContent() instanceof Multipart) {
                extractMultipartBody((Multipart) bodyPart.getContent(), msg);
            }
        }
    }

    private String readContent(Part part) throws MessagingException, IOException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            String charset = extractCharset(part.getContentType());
            return baos.toString(charset);
        }
    }

    private String extractCharset(String contentType) {
        if (contentType == null) return "UTF-8";
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim().toLowerCase();
            if (part.startsWith("charset=")) {
                return part.substring(8).replace("\"", "").trim();
            }
        }
        return "UTF-8";
    }

    private void extractAttachments(MimeMessage message, MailMessage msg) throws MessagingException, IOException {
        List<MailAttachment> attachments = new ArrayList<>();
        List<String> extensions = new ArrayList<>();

        if (message.getContent() instanceof Multipart) {
            extractMultipartAttachments((Multipart) message.getContent(), attachments, extensions);
        }

        msg.setAttachments(attachments);
        msg.setAttachmentCount(attachments.size());
        msg.setAttachmentExtensions(extensions);

        List<String> hashes = new ArrayList<>();
        for (MailAttachment att : attachments) {
            if (att.getContent() != null) {
                att.setHash(computeHash(att.getContent()));
                hashes.add(att.getHash());
            }
        }
        msg.setAttachmentHashes(hashes);
    }

    private void extractMultipartAttachments(Multipart multipart, List<MailAttachment> attachments,
                                              List<String> extensions) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty()) {
                MailAttachment attachment = new MailAttachment();
                attachment.setFileName(decodeHeader(bodyPart.getFileName()));
                attachment.setMimeType(bodyPart.getContentType());
                attachment.setSize(bodyPart.getSize());

                try (InputStream is = bodyPart.getInputStream();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    attachment.setContent(baos.toByteArray());
                }

                attachments.add(attachment);
                String ext = "";
                String name = attachment.getFileName();
                if (name != null && name.contains(".")) {
                    ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                }
                extensions.add(ext);
            }

            if (bodyPart.getContent() instanceof Multipart) {
                extractMultipartAttachments((Multipart) bodyPart.getContent(), attachments, extensions);
            }
        }
    }

    private void extractFlags(MimeMessage message, MailMessage msg) throws MessagingException {
        MailFlags flags = new MailFlags();
        Flags javaFlags = message.getFlags();
        flags.setRead(javaFlags.contains(Flags.Flag.SEEN));
        flags.setImportant(javaFlags.contains(Flags.Flag.FLAGGED));
        flags.setDraft(javaFlags.contains(Flags.Flag.DRAFT));
        flags.setAnswered(javaFlags.contains(Flags.Flag.ANSWERED));
        flags.setDeleted(javaFlags.contains(Flags.Flag.DELETED));
        msg.setFlags(flags);
    }

    private void extractPriority(MimeMessage message, MailMessage msg) throws MessagingException {
        String[] headers = message.getHeader("X-Priority");
        if (headers != null && headers.length > 0) {
            String priority = headers[0].trim();
            if (priority.startsWith("1") || priority.startsWith("2")) {
                msg.setPriority(MailMessage.Priority.HIGH);
            } else if (priority.startsWith("4") || priority.startsWith("5")) {
                msg.setPriority(MailMessage.Priority.LOW);
            } else {
                msg.setPriority(MailMessage.Priority.NORMAL);
            }
        } else {
            msg.setPriority(MailMessage.Priority.NORMAL);
        }
    }

    private void extractReferences(MimeMessage message, MailMessage msg) throws MessagingException {
        String[] refHeaders = message.getHeader("References");
        if (refHeaders != null && refHeaders.length > 0) {
            String refs = refHeaders[0];
            String[] parts = refs.split("\\s+");
            List<String> references = new ArrayList<>();
            for (String ref : parts) {
                ref = ref.replaceAll("[<>]", "").trim();
                if (!ref.isEmpty()) {
                    references.add(ref);
                }
            }
            msg.setReferences(references);
        }
    }

    private String computeHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    private String computeHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    @Override
    public String getFormatName() { return "EML"; }

    @Override
    public String getFormatType() { return "EML"; }
}
