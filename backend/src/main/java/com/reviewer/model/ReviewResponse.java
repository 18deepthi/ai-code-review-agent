package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResponse {
    private String reviewId;
    private String teamId;
    private Instant timestamp;
    private List<ReviewComment> comments = new ArrayList<>();
    private List<String> recalledMemories = new ArrayList<>();
    private int totalIssuesCount;
    private int skippedCount;
    private int activeIssuesCount;

    public ReviewResponse() {
        this.reviewId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public ReviewResponse(String teamId, List<ReviewComment> comments, List<String> recalledMemories) {
        this.reviewId = UUID.randomUUID().toString();
        this.teamId = teamId;
        this.timestamp = Instant.now();
        this.comments = comments != null ? comments : new ArrayList<>();
        this.recalledMemories = recalledMemories != null ? recalledMemories : new ArrayList<>();
        calculateCounts();
    }

    public void calculateCounts() {
        this.totalIssuesCount = comments.size();
        this.skippedCount = (int) comments.stream().filter(ReviewComment::isSkippedDueToMemory).count();
        this.activeIssuesCount = this.totalIssuesCount - this.skippedCount;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public List<ReviewComment> getComments() { return comments; }
    public void setComments(List<ReviewComment> comments) {
        this.comments = comments;
        calculateCounts();
    }

    public List<String> getRecalledMemories() { return recalledMemories; }
    public void setRecalledMemories(List<String> recalledMemories) { this.recalledMemories = recalledMemories; }

    public int getTotalIssuesCount() { return totalIssuesCount; }
    public void setTotalIssuesCount(int totalIssuesCount) { this.totalIssuesCount = totalIssuesCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getActiveIssuesCount() { return activeIssuesCount; }
    public void setActiveIssuesCount(int activeIssuesCount) { this.activeIssuesCount = activeIssuesCount; }
}
