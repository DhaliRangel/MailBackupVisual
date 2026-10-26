package com.mbv.model;

public class SearchResult {

    private MailMessage message;
    private float score;
    private String highlightedSubject;
    private String highlightedBody;

    public SearchResult() {}

    public SearchResult(MailMessage message, float score) {
        this.message = message;
        this.score = score;
    }

    public MailMessage getMessage() { return message; }
    public void setMessage(MailMessage message) { this.message = message; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public String getHighlightedSubject() { return highlightedSubject; }
    public void setHighlightedSubject(String highlightedSubject) { this.highlightedSubject = highlightedSubject; }

    public String getHighlightedBody() { return highlightedBody; }
    public void setHighlightedBody(String highlightedBody) { this.highlightedBody = highlightedBody; }
}
