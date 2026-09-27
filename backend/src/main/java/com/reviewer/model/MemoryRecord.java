package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryRecord {
    private String id;
    private String teamId;
    private String rule;
    private String decision; // "accepted" or "rejected"
    private String sourceSnippetExcerpt;
    private String note;
    private Instant timestamp;
    private Map<String, String> metadata = new HashMap<>();

    public MemoryRecord() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public MemoryRecord(String teamId, String rule, String decision, String sourceSnippetExcerpt, String note) {
        this.id = UUID.randomUUID().toString();
        this.teamId = teamId;
        this.rule = rule;
        this.decision = decision;
        this.sourceSnippetExcerpt = sourceSnippetExcerpt;
        this.note = note;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getSourceSnippetExcerpt() { return sourceSnippetExcerpt; }
    public void setSourceSnippetExcerpt(String sourceSnippetExcerpt) { this.sourceSnippetExcerpt = sourceSnippetExcerpt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
