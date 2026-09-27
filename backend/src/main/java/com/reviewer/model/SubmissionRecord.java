package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionRecord {
    private String id;
    private String teamId;
    private String codeSnippet;
    private String language;
    private Instant timestamp;
    private int totalIssuesCount;
    private int skippedCount;
    private int activeIssuesCount;
    private List<ReviewComment> comments = new ArrayList<>();
    private List<String> recalledMemories = new ArrayList<>();

    public SubmissionRecord() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public SubmissionRecord(String teamId, String codeSnippet, String language, ReviewResponse reviewResponse) {
        this.id = reviewResponse.getReviewId() != null ? reviewResponse.getReviewId() : UUID.randomUUID().toString();
        this.teamId = teamId;
        this.codeSnippet = codeSnippet;
        this.language = language;
        this.timestamp = reviewResponse.getTimestamp() != null ? reviewResponse.getTimestamp() : Instant.now();
        this.comments = reviewResponse.getComments();
        this.recalledMemories = reviewResponse.getRecalledMemories();
        this.totalIssuesCount = reviewResponse.getTotalIssuesCount();
        this.skippedCount = reviewResponse.getSkippedCount();
        this.activeIssuesCount = reviewResponse.getActiveIssuesCount();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public int getTotalIssuesCount() { return totalIssuesCount; }
    public void setTotalIssuesCount(int totalIssuesCount) { this.totalIssuesCount = totalIssuesCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getActiveIssuesCount() { return activeIssuesCount; }
    public void setActiveIssuesCount(int activeIssuesCount) { this.activeIssuesCount = activeIssuesCount; }

    public List<ReviewComment> getComments() { return comments; }
    public void setComments(List<ReviewComment> comments) { this.comments = comments; }

    public List<String> getRecalledMemories() { return recalledMemories; }
    public void setRecalledMemories(List<String> recalledMemories) { this.recalledMemories = recalledMemories; }
}
