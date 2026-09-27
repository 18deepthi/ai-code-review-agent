package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackRequest {
    private String teamId;
    private String reviewId;
    private String commentId;
    private String decision; // "accepted" or "rejected"
    private String note;
    private String rule; // human-readable rule summary (e.g. "Do not flag field injection")
    private String ruleKey; // e.g. "field_injection", "n_plus_one", "empty_catch"
    private String sourceSnippetExcerpt;

    public FeedbackRequest() {}

    public FeedbackRequest(String teamId, String reviewId, String commentId, String decision, String note) {
        this.teamId = teamId;
        this.reviewId = reviewId;
        this.commentId = commentId;
        this.decision = decision;
        this.note = note;
    }

    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public String getSourceSnippetExcerpt() { return sourceSnippetExcerpt; }
    public void setSourceSnippetExcerpt(String sourceSnippetExcerpt) { this.sourceSnippetExcerpt = sourceSnippetExcerpt; }
}
