package com.mbv.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SearchQuery {

    private String text;
    private String from;
    private String to;
    private String subject;
    private String body;
    private String attachmentType;
    private Boolean hasAttachment;
    private LocalDate afterDate;
    private LocalDate beforeDate;
    private String domain;
    private Long minSize;
    private Long maxSize;
    private Boolean onlyHtml;
    private Boolean onlyPlainText;
    private Boolean onlyConversations;
    private Boolean onlyUnreplied;
    private Boolean onlyReplied;
    private List<String> folders;
    private List<String> archiveTypes;

    public SearchQuery() {
        this.folders = new ArrayList<>();
        this.archiveTypes = new ArrayList<>();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    public Boolean getHasAttachment() { return hasAttachment; }
    public void setHasAttachment(Boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    public LocalDate getAfterDate() { return afterDate; }
    public void setAfterDate(LocalDate afterDate) { this.afterDate = afterDate; }

    public LocalDate getBeforeDate() { return beforeDate; }
    public void setBeforeDate(LocalDate beforeDate) { this.beforeDate = beforeDate; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public Long getMinSize() { return minSize; }
    public void setMinSize(Long minSize) { this.minSize = minSize; }

    public Long getMaxSize() { return maxSize; }
    public void setMaxSize(Long maxSize) { this.maxSize = maxSize; }

    public Boolean getOnlyHtml() { return onlyHtml; }
    public void setOnlyHtml(Boolean onlyHtml) { this.onlyHtml = onlyHtml; }

    public Boolean getOnlyPlainText() { return onlyPlainText; }
    public void setOnlyPlainText(Boolean onlyPlainText) { this.onlyPlainText = onlyPlainText; }

    public Boolean getOnlyConversations() { return onlyConversations; }
    public void setOnlyConversations(Boolean onlyConversations) { this.onlyConversations = onlyConversations; }

    public Boolean getOnlyUnreplied() { return onlyUnreplied; }
    public void setOnlyUnreplied(Boolean onlyUnreplied) { this.onlyUnreplied = onlyUnreplied; }

    public Boolean getOnlyReplied() { return onlyReplied; }
    public void setOnlyReplied(Boolean onlyReplied) { this.onlyReplied = onlyReplied; }

    public List<String> getFolders() { return folders; }
    public void setFolders(List<String> folders) { this.folders = folders; }

    public List<String> getArchiveTypes() { return archiveTypes; }
    public void setArchiveTypes(List<String> archiveTypes) { this.archiveTypes = archiveTypes; }

    public boolean hasFilters() {
        return from != null || to != null || subject != null || body != null ||
                attachmentType != null || hasAttachment != null ||
                afterDate != null || beforeDate != null || domain != null ||
                minSize != null || maxSize != null ||
                onlyHtml != null || onlyPlainText != null ||
                onlyConversations != null || onlyUnreplied != null || onlyReplied != null;
    }
}
