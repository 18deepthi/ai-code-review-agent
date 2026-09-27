package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewComment {
    private String id;
    private String issue;
    private String severity; // "low", "medium", "high"
    private String suggestion;
    private boolean skippedDueToMemory;
    private String memoryReason; // e.g. "Skipped — team preference from [date]"
    private String ruleKey;
    private String lineRange;

    public ReviewComment() {
        this.id = UUID.randomUUID().toString();
    }

    public ReviewComment(String issue, String severity, String suggestion, boolean skippedDueToMemory, String memoryReason) {
        this.id = UUID.randomUUID().toString();
        this.issue = issue;
        this.severity = severity;
        this.suggestion = suggestion;
        this.skippedDueToMemory = skippedDueToMemory;
        this.memoryReason = memoryReason;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public boolean isSkippedDueToMemory() { return skippedDueToMemory; }
    public void setSkippedDueToMemory(boolean skippedDueToMemory) { this.skippedDueToMemory = skippedDueToMemory; }

    public String getMemoryReason() { return memoryReason; }
    public void setMemoryReason(String memoryReason) { this.memoryReason = memoryReason; }

    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }

    public String getLineRange() { return lineRange; }
    public void setLineRange(String lineRange) { this.lineRange = lineRange; }
}
