package com.reviewer.service;

import com.reviewer.model.FeedbackRequest;
import com.reviewer.model.FeedbackResponse;
import com.reviewer.model.MemoryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final HindsightClient hindsightClient;

    public FeedbackService(HindsightClient hindsightClient) {
        this.hindsightClient = hindsightClient;
    }

    public FeedbackResponse processFeedback(FeedbackRequest request) {
        String teamId = (request.getTeamId() != null && !request.getTeamId().isBlank())
                ? request.getTeamId().trim()
                : "team-alpha";
        String decision = request.getDecision() != null ? request.getDecision().trim().toLowerCase() : "accepted";
        String rawRule = request.getRule();
        final String effectiveRuleText = (rawRule != null && !rawRule.isBlank())
                ? rawRule
                : deriveRuleFromNoteOrContext(request);

        String ruleKey = deriveRuleKey(request.getRuleKey(), effectiveRuleText, request.getNote(), request.getSourceSnippetExcerpt());
        log.info("Processing feedback for team '{}': decision='{}', ruleKey='{}', rule='{}'", teamId, decision, ruleKey, effectiveRuleText);

        String memoryContent = buildMemoryStatement(decision, effectiveRuleText, request.getNote(), ruleKey);

        // Deduplication Check:
        // Before retaining a new memory, check if an existing memory for the same team + same issue type already exists,
        // and avoid creating near-duplicate entries on repeated "Reject & Remember" clicks for the same issue.
        List<MemoryRecord> existingMemories = hindsightClient.listMemories(teamId);
        boolean isDuplicate = checkDuplicateMemory(existingMemories, ruleKey, decision, effectiveRuleText);

        if (isDuplicate) {
            log.info("Deduplication: Preference for team '{}' and issue type '{}' ('{}') already exists. Skipping duplicate write.",
                    teamId, ruleKey, decision);
            return new FeedbackResponse(true,
                    "Team preference already recorded for this issue type (deduplicated).",
                    UUID.randomUUID().toString(), teamId, memoryContent, decision);
        }

        // Build metadata for Hindsight
        Map<String, String> metadata = new HashMap<>();
        metadata.put("teamId", teamId);
        metadata.put("decision", decision);
        metadata.put("ruleKey", ruleKey);
        metadata.put("reviewId", request.getReviewId() != null ? request.getReviewId() : "");
        metadata.put("commentId", request.getCommentId() != null ? request.getCommentId() : "");
        metadata.put("note", request.getNote() != null ? request.getNote() : "");
        metadata.put("sourceSnippetExcerpt", request.getSourceSnippetExcerpt() != null ? request.getSourceSnippetExcerpt() : "");

        // Write to Hindsight memory bank
        boolean stored = hindsightClient.writeMemory(teamId, memoryContent, metadata);

        String memoryId = UUID.randomUUID().toString();
        String message = "rejected".equalsIgnoreCase(decision)
                ? "Learned preference: Reviewer will skip flagging this issue for team " + teamId + " in future reviews."
                : "Reinforced standard: Reviewer will continue to enforce this guideline for team " + teamId + ".";

        return new FeedbackResponse(stored, message, memoryId, teamId, memoryContent, decision);
    }

    private String buildMemoryStatement(String decision, String ruleText, String note, String ruleKey) {
        StringBuilder sb = new StringBuilder();
        if ("rejected".equalsIgnoreCase(decision)) {
            sb.append("OVERRIDDEN TEAM PREFERENCE (DO NOT FLAG) [#").append(ruleKey).append("]: ").append(ruleText);
            if (note != null && !note.isBlank()) {
                sb.append(". Justification / Team Convention: ").append(note);
            }
        } else {
            sb.append("ENFORCED TEAM GUIDELINE [#").append(ruleKey).append("]: ").append(ruleText);
            if (note != null && !note.isBlank()) {
                sb.append(". Team Note: ").append(note);
            }
        }
        return sb.toString();
    }

    public static String deriveRuleKey(String ruleKey, String ruleText, String note, String excerpt) {
        if (ruleKey != null && !ruleKey.isBlank() && !ruleKey.equalsIgnoreCase("other")) {
            return ruleKey.trim().toLowerCase();
        }
        String combined = ((ruleText != null ? ruleText : "") + " "
                + (note != null ? note : "") + " "
                + (excerpt != null ? excerpt : "")).toLowerCase();

        if (combined.contains("field injection") || combined.contains("autowired") || combined.contains("constructor injection")) {
            return "field_injection";
        }
        if (combined.contains("empty catch") || combined.contains("swallowed exception") || combined.contains("catch block")) {
            return "empty_catch";
        }
        if (combined.contains("n+1") || combined.contains("n + 1") || combined.contains("loop query") || combined.contains("batch query")) {
            return "n_plus_one";
        }
        if (combined.contains("try-with-resources") || combined.contains("resource leak") || combined.contains("unclosed") || combined.contains("autocloseable")) {
            return "try_with_resources";
        }
        if (combined.contains("null check") || combined.contains("optional") || combined.contains("nullpointer")) {
            return "null_check";
        }
        if (combined.contains("magic number") || combined.contains("constant")) {
            return "magic_number";
        }
        return "general_rule";
    }

    private boolean checkDuplicateMemory(List<MemoryRecord> existingMemories, String ruleKey, String decision, String ruleText) {
        if (existingMemories == null || existingMemories.isEmpty()) {
            return false;
        }

        for (MemoryRecord rec : existingMemories) {
            String recDecision = rec.getDecision() != null ? rec.getDecision().toLowerCase() : "accepted";
            if (!recDecision.equalsIgnoreCase(decision)) {
                continue;
            }

            if (rec.getMetadata() != null) {
                String metaKey = rec.getMetadata().get("ruleKey");
                if (metaKey != null && metaKey.equalsIgnoreCase(ruleKey)) {
                    return true;
                }
            }

            String recDerivedKey = deriveRuleKey(null, rec.getRule(), rec.getNote(), rec.getSourceSnippetExcerpt());
            if (recDerivedKey.equalsIgnoreCase(ruleKey) && !ruleKey.equals("general_rule")) {
                return true;
            }

            if (rec.getRule() != null && ruleText != null && rec.getRule().toLowerCase().contains(ruleText.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String deriveRuleFromNoteOrContext(FeedbackRequest request) {
        if (request.getNote() != null && !request.getNote().isBlank()) {
            return request.getNote();
        }
        return "Custom team preference regarding code structure";
    }
}
