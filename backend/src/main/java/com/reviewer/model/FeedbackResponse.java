package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackResponse {
    private boolean success;
    private String message;
    private String memoryId;
    private String teamId;
    private String rule;
    private String decision;
    private Instant timestamp;

    public FeedbackResponse() {
        this.timestamp = Instant.now();
    }

    public FeedbackResponse(boolean success, String message, String memoryId, String teamId, String rule, String decision) {
        this.success = success;
        this.message = message;
        this.memoryId = memoryId;
        this.teamId = teamId;
        this.rule = rule;
        this.decision = decision;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMemoryId() { return memoryId; }
    public void setMemoryId(String memoryId) { this.memoryId = memoryId; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
