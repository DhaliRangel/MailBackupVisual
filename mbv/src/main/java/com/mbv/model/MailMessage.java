package com.mbv.model;

import java.time.LocalDateTime;
import java.util.*;

public class MailMessage {

    private String id;
    private String messageId;
    private String inReplyTo;
    private List<String> references;
    private String threadId;
    private String subject;
    private String plainBody;
    private String htmlBody;
    private Contact sender;
    private List<Contact> to;
    private List<Contact> cc;
    private List<Contact> bcc;
    private LocalDateTime sentDate;
    private LocalDateTime receivedDate;
    private Priority priority;
    private MailFlags flags;
    private List<MailAttachment> attachments;
    private String folder;
    private String sourceArchive;
    private String sourceArchiveType;
    private String originalPath;
    private long size;
    private String charset;
    private String mimeType;
    private int attachmentCount;
    private List<String> attachmentExtensions;
    private String hash;
    private List<String> attachmentHashes;
    private String label;
    private String gmailLabels;
    private LocalDateTime importDate;
    private boolean indexed;
    private int indexVersion;

    public MailMessage() {
        this.to = new ArrayList<>();
        this.cc = new ArrayList<>();
        this.bcc = new ArrayList<>();
        this.references = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.attachmentExtensions = new ArrayList<>();
        this.attachmentHashes = new ArrayList<>();
        this.flags = new MailFlags();
        this.importDate = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getInReplyTo() { return inReplyTo; }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }

    public List<String> getReferences() { return references; }
    public void setReferences(List<String> references) { this.references = references; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getPlainBody() { return plainBody; }
    public void setPlainBody(String plainBody) { this.plainBody = plainBody; }

    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }

    public Contact getSender() { return sender; }
    public void setSender(Contact sender) { this.sender = sender; }

    public List<Contact> getTo() { return to; }
    public void setTo(List<Contact> to) { this.to = to; }

    public List<Contact> getCc() { return cc; }
    public void setCc(List<Contact> cc) { this.cc = cc; }

    public List<Contact> getBcc() { return bcc; }
    public void setBcc(List<Contact> bcc) { this.bcc = bcc; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public LocalDateTime getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDateTime receivedDate) { this.receivedDate = receivedDate; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public MailFlags getFlags() { return flags; }
    public void setFlags(MailFlags flags) { this.flags = flags; }

    public List<MailAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<MailAttachment> attachments) { this.attachments = attachments; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getSourceArchive() { return sourceArchive; }
    public void setSourceArchive(String sourceArchive) { this.sourceArchive = sourceArchive; }

    public String getSourceArchiveType() { return sourceArchiveType; }
    public void setSourceArchiveType(String sourceArchiveType) { this.sourceArchiveType = sourceArchiveType; }

    public String getOriginalPath() { return originalPath; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public int getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(int attachmentCount) { this.attachmentCount = attachmentCount; }

    public List<String> getAttachmentExtensions() { return attachmentExtensions; }
    public void setAttachmentExtensions(List<String> attachmentExtensions) { this.attachmentExtensions = attachmentExtensions; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public List<String> getAttachmentHashes() { return attachmentHashes; }
    public void setAttachmentHashes(List<String> attachmentHashes) { this.attachmentHashes = attachmentHashes; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getGmailLabels() { return gmailLabels; }
    public void setGmailLabels(String gmailLabels) { this.gmailLabels = gmailLabels; }

    public LocalDateTime getImportDate() { return importDate; }
    public void setImportDate(LocalDateTime importDate) { this.importDate = importDate; }

    public boolean isIndexed() { return indexed; }
    public void setIndexed(boolean indexed) { this.indexed = indexed; }

    public int getIndexVersion() { return indexVersion; }
    public void setIndexVersion(int indexVersion) { this.indexVersion = indexVersion; }

    public String getSenderDomain() {
        return sender != null && sender.getEmail() != null
                ? sender.getEmail().substring(sender.getEmail().indexOf('@') + 1)
                : "";
    }

    public String getRecipientDomains() {
        StringBuilder sb = new StringBuilder();
        for (Contact c : to) {
            if (c.getEmail() != null && c.getEmail().contains("@")) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(c.getEmail().substring(c.getEmail().indexOf('@') + 1));
            }
        }
        return sb.toString();
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public boolean isHtml() {
        return htmlBody != null && !htmlBody.isEmpty();
    }

    public enum Priority {
        HIGH, NORMAL, LOW
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailMessage that = (MailMessage) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
