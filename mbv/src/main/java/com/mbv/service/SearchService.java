package com.mbv.service;

import com.mbv.model.MailMessage;
import com.mbv.model.SearchQuery;
import com.mbv.model.SearchResult;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Pattern FIELD_PATTERN =
            Pattern.compile("(from|to|subject|body|has|attachment|after|before|is|in):\\s*\"?([^\"\\s]+)\"?");

    @Autowired
    private IndexingService indexingService;

    public List<SearchResult> search(String query, int maxResults) throws IOException, ParseException {
        List<MailMessage> messages = indexingService.search(query, maxResults);
        return messages.stream()
                .map(msg -> new SearchResult(msg, 1.0f))
                .collect(Collectors.toList());
    }

    public List<SearchResult> searchWithQuery(SearchQuery searchQuery, int maxResults)
            throws IOException, ParseException {

        String textQuery = buildTextQuery(searchQuery);
        Map<String, String> fieldFilters = buildFieldFilters(searchQuery);

        List<MailMessage> messages = indexingService.searchWithFilters(textQuery, fieldFilters, maxResults);

        return messages.stream()
                .filter(msg -> applyAdditionalFilters(msg, searchQuery))
                .map(msg -> new SearchResult(msg, 1.0f))
                .collect(Collectors.toList());
    }

    public List<SearchResult> parseAndSearch(String rawQuery, int maxResults) throws IOException, ParseException {
        SearchQuery query = parseGoogleLikeQuery(rawQuery);
        return searchWithQuery(query, maxResults);
    }

    private SearchQuery parseGoogleLikeQuery(String rawQuery) {
        SearchQuery query = new SearchQuery();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return query;
        }

        String remaining = rawQuery;
        Matcher matcher = FIELD_PATTERN.matcher(rawQuery);

        while (matcher.find()) {
            String field = matcher.group(1).toLowerCase();
            String value = matcher.group(2);

            switch (field) {
                case "from" -> query.setFrom(value);
                case "to" -> query.setTo(value);
                case "subject" -> query.setSubject(value);
                case "body" -> query.setBody(value);
                case "has" -> {
                    if ("attachment".equalsIgnoreCase(value)) {
                        query.setHasAttachment(true);
                    }
                }
                case "attachment" -> query.setAttachmentType(value);
                case "after" -> {
                    try {
                        query.setAfterDate(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception e) { /* ignore */ }
                }
                case "before" -> {
                    try {
                        query.setBeforeDate(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception e) { /* ignore */ }
                }
                case "is" -> {
                    if ("unread".equalsIgnoreCase(value)) {
                        query.setOnlyUnreplied(true);
                    }
                }
            }

            remaining = remaining.replace(matcher.group(0), "").trim();
        }

        // Check for quoted phrase
        Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher quoteMatcher = quotePattern.matcher(remaining);
        if (quoteMatcher.find()) {
            query.setText(quoteMatcher.group(1));
        } else if (!remaining.trim().isEmpty()) {
            query.setText(remaining.trim());
        }

        return query;
    }

    private String buildTextQuery(SearchQuery query) {
        StringBuilder sb = new StringBuilder();

        if (query.getText() != null && !query.getText().isEmpty()) {
            sb.append(query.getText());
        }

        if (query.getSubject() != null && !query.getSubject().isEmpty()) {
            if (!sb.isEmpty()) sb.append(" AND ");
            sb.append("subject:").append(query.getSubject());
        }

        if (query.getBody() != null && !query.getBody().isEmpty()) {
            if (!sb.isEmpty()) sb.append(" AND ");
            sb.append("plainBody:").append(query.getBody());
        }

        return sb.toString();
    }

    private Map<String, String> buildFieldFilters(SearchQuery query) {
        Map<String, String> filters = new HashMap<>();

        if (query.getFrom() != null && !query.getFrom().isEmpty()) {
            filters.put("senderEmail", query.getFrom());
        }

        if (query.getAttachmentType() != null && !query.getAttachmentType().isEmpty()) {
            filters.put("attachmentExtensions", query.getAttachmentType());
        }

        return filters;
    }

    private boolean applyAdditionalFilters(MailMessage msg, SearchQuery query) {
        if (query.getFrom() != null && !query.getFrom().isEmpty()) {
            if (msg.getSender() == null ||
                    !msg.getSender().getEmail().toLowerCase().contains(query.getFrom().toLowerCase())) {
                return false;
            }
        }

        if (query.getTo() != null && !query.getTo().isEmpty()) {
            boolean matches = msg.getTo().stream()
                    .anyMatch(c -> c.getEmail().toLowerCase().contains(query.getTo().toLowerCase()));
            if (!matches) return false;
        }

        if (query.getAfterDate() != null && msg.getSentDate() != null) {
            if (msg.getSentDate().toLocalDate().isBefore(query.getAfterDate())) {
                return false;
            }
        }

        if (query.getBeforeDate() != null && msg.getSentDate() != null) {
            if (msg.getSentDate().toLocalDate().isAfter(query.getBeforeDate())) {
                return false;
            }
        }

        if (query.getHasAttachment() != null && query.getHasAttachment()) {
            if (!msg.hasAttachments()) return false;
        }

        if (query.getOnlyHtml() != null && query.getOnlyHtml()) {
            if (!msg.isHtml()) return false;
        }

        if (query.getOnlyPlainText() != null && query.getOnlyPlainText()) {
            if (msg.isHtml()) return false;
        }

        if (query.getDomain() != null && !query.getDomain().isEmpty()) {
            String senderDomain = msg.getSenderDomain();
            if (!senderDomain.equalsIgnoreCase(query.getDomain())) {
                return false;
            }
        }

        return true;
    }

    public List<String> suggestCompletions(String partial) {
        // Basic suggestions - could be enhanced with Lucene suggest
        List<String> suggestions = new ArrayList<>();
        if (partial != null && partial.length() > 2) {
            suggestions.add("from:" + partial);
            suggestions.add("subject:" + partial);
            suggestions.add("attachment:" + partial);
        }
        return suggestions;
    }
}
