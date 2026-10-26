package com.mbv.model;

import java.util.ArrayList;
import java.util.List;

public class MailThread {

    private String threadId;
    private String subject;
    private List<MailMessage> messages;
    private int depth;
    private MailMessage rootMessage;

    public MailThread() {
        this.messages = new ArrayList<>();
    }

    public MailThread(String threadId) {
        this.threadId = threadId;
        this.messages = new ArrayList<>();
    }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public List<MailMessage> getMessages() { return messages; }
    public void setMessages(List<MailMessage> messages) { this.messages = messages; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public MailMessage getRootMessage() { return rootMessage; }
    public void setRootMessage(MailMessage rootMessage) { this.rootMessage = rootMessage; }

    public void addMessage(MailMessage message) {
        this.messages.add(message);
        message.setThreadId(threadId);
    }

    public int getMessageCount() {
        return messages.size();
    }

    public MailMessage getLatestMessage() {
        if (messages.isEmpty()) return null;
        return messages.get(messages.size() - 1);
    }

    public String getParticipantSummary() {
        java.util.Set<String> participants = new java.util.LinkedHashSet<>();
        for (MailMessage msg : messages) {
            if (msg.getSender() != null) {
                participants.add(msg.getSender().getDisplayName());
            }
            for (Contact c : msg.getTo()) {
                participants.add(c.getDisplayName());
            }
        }
        return String.join(", ", participants);
    }
}
