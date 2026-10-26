package com.mbv.parser;

import com.mbv.model.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Component
public class MboxParser implements MailParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".mbox") || name.endsWith(".mbx");
    }

    @Override
    public Stream<MailMessage> parse(Path file) throws IOException {
        List<MailMessage> messages = new ArrayList<>();
        List<String> rawMessages = splitMboxFile(file);

        for (String rawMessage : rawMessages) {
            try {
                MailMessage mailMessage = parseRawMessage(rawMessage);
                if (mailMessage != null) {
                    mailMessage.setSourceArchive(file.getFileName().toString());
                    mailMessage.setSourceArchiveType("MBOX");
                    mailMessage.setOriginalPath(file.toString());
                    messages.add(mailMessage);
                }
            } catch (Exception e) {
                System.err.println("Error parsing MBOX message: " + e.getMessage());
            }
        }

        return messages.stream();
    }

    private List<String> splitMboxFile(Path file) throws IOException {
        List<String> messages = new ArrayList<>();
        StringBuilder currentMessage = new StringBuilder();
        boolean inMessage = false;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("From ")) {
                    if (inMessage && currentMessage.length() > 0) {
                        messages.add(currentMessage.toString());
                        currentMessage.setLength(0);
                    }
                    inMessage = true;
                    continue;
                }
                if (inMessage) {
                    currentMessage.append(line).append("\n");
                }
            }
            if (inMessage && currentMessage.length() > 0) {
                messages.add(currentMessage.toString());
            }
        }

        return messages;
    }

    private MailMessage parseRawMessage(String rawMessage) throws MessagingException, IOException {
        MimeMessage mimeMessage = createMimeMessage(rawMessage);
        if (mimeMessage == null) return null;

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

        msg.setCharset(mimeMessage.getContentType());
        msg.setSize(rawMessage.getBytes(StandardCharsets.UTF_8).length);
        msg.setHash(computeHash(rawMessage));

        return msg;
    }

    private MimeMessage createMimeMessage(String rawMessage) {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            return new MimeMessage(session, new ByteArrayInputStream(rawMessage.getBytes(StandardCharsets.UTF_8)));
        } catch (MessagingException e) {
            return null;
        }
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
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
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
    public String getFormatName() { return "MBOX"; }

    @Override
    public String getFormatType() { return "MBOX"; }
}
