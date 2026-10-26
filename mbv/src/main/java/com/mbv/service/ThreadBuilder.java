package com.mbv.service;

import com.mbv.model.MailMessage;
import com.mbv.model.MailThread;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThreadBuilder {

    private Map<String, MailThread> threads = new HashMap<>();
    private Map<String, String> messageIdToThreadId = new HashMap<>();

    public List<MailThread> buildThreads(List<MailMessage> messages) {
        threads.clear();
        messageIdToThreadId.clear();

        // First pass: create threads based on In-Reply-To and References
        for (MailMessage msg : messages) {
            String threadId = determineThreadId(msg);
            getOrCreateThread(threadId).addMessage(msg);
            messageIdToThreadId.put(msg.getMessageId(), threadId);
        }

        // Second pass: normalize subject-based threads
        normalizeBySubject();

        // Third pass: set root messages and compute depth
        for (MailThread thread : threads.values()) {
            setRootAndDepth(thread);
        }

        return threads.values().stream()
                .sorted(Comparator.comparingInt((MailThread t) -> t.getMessageCount()).reversed())
                .collect(Collectors.toList());
    }

    private String determineThreadId(MailMessage msg) {
        // If the message has In-Reply-To, it belongs to an existing thread
        if (msg.getInReplyTo() != null && !msg.getInReplyTo().isEmpty()) {
            String parentThreadId = messageIdToThreadId.get(msg.getInReplyTo());
            if (parentThreadId != null) {
                return parentThreadId;
            }
            return msg.getInReplyTo();
        }

        // If it has references, use the first reference as thread root
        if (msg.getReferences() != null && !msg.getReferences().isEmpty()) {
            String rootRef = msg.getReferences().get(0);
            String rootThreadId = messageIdToThreadId.get(rootRef);
            if (rootThreadId != null) {
                return rootThreadId;
            }
            return rootRef;
        }

        // Otherwise, create a new thread based on message ID
        return msg.getMessageId();
    }

    private MailThread getOrCreateThread(String threadId) {
        return threads.computeIfAbsent(threadId, id -> {
            MailThread thread = new MailThread(id);
            return thread;
        });
    }

    private void normalizeBySubject() {
        // Group threads by normalized subject
        Map<String, List<MailThread>> subjectGroups = new HashMap<>();

        for (MailThread thread : threads.values()) {
            String normalizedSubject = normalizeSubject(thread.getSubject());
            if (normalizedSubject != null && !normalizedSubject.isEmpty()) {
                subjectGroups.computeIfAbsent(normalizedSubject, k -> new ArrayList<>()).add(thread);
            }
        }

        // Merge threads with same subject
        for (var entry : subjectGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                MailThread mainThread = entry.getValue().get(0);
                for (int i = 1; i < entry.getValue().size(); i++) {
                    MailThread otherThread = entry.getValue().get(i);
                    for (MailMessage msg : otherThread.getMessages()) {
                        mainThread.addMessage(msg);
                        messageIdToThreadId.put(msg.getMessageId(), mainThread.getThreadId());
                    }
                    threads.remove(otherThread.getThreadId());
                }
            }
        }
    }

    private String normalizeSubject(String subject) {
        if (subject == null) return null;
        return subject
                .replaceAll("(?i)^re:\\s*", "")
                .replaceAll("(?i)^fwd?:\\s*", "")
                .replaceAll("(?i)^ris:\\s*", "")
                .replaceAll("(?i)^inoltra:\\s*", "")
                .trim()
                .toLowerCase();
    }

    private void setRootAndDepth(MailThread thread) {
        Set<String> repliedIds = new HashSet<>();
        for (MailMessage msg : thread.getMessages()) {
            if (msg.getInReplyTo() != null) {
                repliedIds.add(msg.getInReplyTo());
            }
            if (msg.getReferences() != null) {
                repliedIds.addAll(msg.getReferences());
            }
        }

        // The root message is the one not referenced by others
        MailMessage root = null;
        for (MailMessage msg : thread.getMessages()) {
            if (!repliedIds.contains(msg.getMessageId())) {
                root = msg;
                break;
            }
        }

        if (root == null) {
            // Fallback: earliest message
            root = thread.getMessages().stream()
                    .min(Comparator.comparing(MailMessage::getSentDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(thread.getMessages().get(0));
        }

        thread.setRootMessage(root);
        thread.setSubject(root.getSubject());

        // Compute depth for each message
        Map<String, Integer> depthMap = new HashMap<>();
        computeDepth(thread.getMessages(), root, 0, depthMap, new HashSet<>());
    }

    private void computeDepth(List<MailMessage> messages, MailMessage current, int depth,
                               Map<String, Integer> depthMap, Set<String> visited) {
        if (current == null || visited.contains(current.getMessageId())) return;
        visited.add(current.getMessageId());
        depthMap.put(current.getMessageId(), depth);

        for (MailMessage msg : messages) {
            if (current.getMessageId().equals(msg.getInReplyTo())) {
                computeDepth(messages, msg, depth + 1, depthMap, visited);
            }
        }
    }

    public MailThread getThreadForMessage(String messageId) {
        String threadId = messageIdToThreadId.get(messageId);
        return threadId != null ? threads.get(threadId) : null;
    }

    public void clear() {
        threads.clear();
        messageIdToThreadId.clear();
    }

    public int getThreadCount() {
        return threads.size();
    }
}
