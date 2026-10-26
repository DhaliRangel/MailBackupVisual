package com.mbv.model;

import java.time.LocalDateTime;

public class MailFlags {

    private boolean read;
    private boolean important;
    private boolean draft;
    private boolean answered;
    private boolean forwarded;
    private boolean deleted;
    private boolean flagged;
    private boolean spam;

    public MailFlags() {}

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public boolean isImportant() { return important; }
    public void setImportant(boolean important) { this.important = important; }

    public boolean isDraft() { return draft; }
    public void setDraft(boolean draft) { this.draft = draft; }

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }

    public boolean isForwarded() { return forwarded; }
    public void setForwarded(boolean forwarded) { this.forwarded = forwarded; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean isSpam() { return spam; }
    public void setSpam(boolean spam) { this.spam = spam; }
}
