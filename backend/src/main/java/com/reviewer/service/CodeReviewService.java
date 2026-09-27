package com.reviewer.service;

import com.reviewer.model.ReviewComment;
import com.reviewer.model.ReviewRequest;
import com.reviewer.model.ReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final HindsightClient hindsightClient;
    private final GroqClient groqClient;
    private final SubmissionService submissionService;

    public CodeReviewService(HindsightClient hindsightClient, GroqClient groqClient, SubmissionService submissionService) {
        this.hindsightClient = hindsightClient;
        this.groqClient = groqClient;
        this.submissionService = submissionService;
    }

    public ReviewResponse performReview(ReviewRequest request) {
        String teamId = (request.getTeamId() != null && !request.getTeamId().isBlank())
                ? request.getTeamId().trim()
                : "team-alpha";
        String code = request.getCodeSnippet() != null ? request.getCodeSnippet() : "";
        String language = request.getLanguage() != null ? request.getLanguage() : "java";

        log.info("Starting review for team: '{}', code length: {} chars", teamId, code.length());

        // Step 1: Extract snippet key characteristics to build a targeted memory recall query
        String recallQuery = extractSnippetCharacteristics(code);
        log.info("Targeted recall query for team '{}': '{}'", teamId, recallQuery);

        // Step 2: Recall relevant team memories from Hindsight
        List<String> recalledMemories = hindsightClient.recallMemory(teamId, recallQuery);
        log.info("Recalled {} relevant team memories for review", recalledMemories.size());

        // Step 3: Invoke Groq LLM (with injected recalled memories and retry logic)
        List<ReviewComment> comments = groqClient.reviewCode(code, recalledMemories);
        if (comments == null) {
            comments = new ArrayList<>();
        }

        // Step 4: Deterministic Decision Logic: Match recalled memories to comments and mark Skipped vs Actionable
        applyMemoryOverrides(comments, recalledMemories);

        // Step 5: Construct review response (recalculates counts: total, skipped, actionable)
        ReviewResponse response = new ReviewResponse(teamId, comments, recalledMemories);

        // Step 6: Persist in submission history
        submissionService.recordSubmission(teamId, code, language, response);

        return response;
    }

    /**
     * Matches recalled team memories against detected review comments.
     * When a recalled memory expresses an override/preference for an issue type,
     * marks that comment as Skipped (skippedDueToMemory = true) with the specific reason.
     */
    public void applyMemoryOverrides(List<ReviewComment> comments, List<String> recalledMemories) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        for (ReviewComment comment : comments) {
            normalizeCommentRuleKey(comment);
        }

        if (recalledMemories == null || recalledMemories.isEmpty()) {
            return;
        }

        for (ReviewComment comment : comments) {
            if (comment.isSkippedDueToMemory()) {
                if (comment.getMemoryReason() == null || comment.getMemoryReason().isBlank()) {
                    comment.setMemoryReason("Skipped — team preference from past feedback");
                }
                continue;
            }

            for (String memory : recalledMemories) {
                if (doesMemoryOverrideComment(memory, comment)) {
                    comment.setSkippedDueToMemory(true);
                    comment.setMemoryReason(formatSkipReason(memory, comment));
                    log.info("Suppressed comment [{}] ('{}') due to recalled team memory: '{}'",
                            comment.getRuleKey(), comment.getIssue(), memory);
                    break;
                }
            }
        }
    }

    /**
     * Determines whether a recalled memory overrides / suppresses a given comment.
     */
    public boolean doesMemoryOverrideComment(String memory, ReviewComment comment) {
        if (memory == null || memory.isBlank()) {
            return false;
        }

        String memLower = memory.toLowerCase();
        String ruleKey = (comment.getRuleKey() != null) ? comment.getRuleKey().toLowerCase() : "";

        // Memory must convey an override, permission, suppression, or preference
        boolean isOverridePreference = memLower.contains("overridden")
                || memLower.contains("do not flag")
                || memLower.contains("permitted")
                || memLower.contains("allowed")
                || memLower.contains("allow")
                || memLower.contains("skip")
                || memLower.contains("suppress")
                || memLower.contains("suppressed")
                || memLower.contains("preference")
                || memLower.contains("accept")
                || memLower.contains("accepted")
                || memLower.contains("rejected");

        if (!isOverridePreference) {
            return false;
        }

        // 1. Direct tag match (e.g. [#field_injection], #field_injection, or field_injection)
        if (!ruleKey.isEmpty() && (memLower.contains("[#" + ruleKey + "]")
                || memLower.contains("#" + ruleKey)
                || memLower.contains(ruleKey))) {
            return true;
        }

        // 2. Domain keyword matching per ruleKey
        switch (ruleKey) {
            case "field_injection":
                if (memLower.contains("field injection") || memLower.contains("autowired") || memLower.contains("constructor injection")) {
                    return true;
                }
                break;
            case "empty_catch":
                if (memLower.contains("empty catch") || memLower.contains("swallowed exception") || memLower.contains("catch block") || memLower.contains("ignore exception")) {
                    return true;
                }
                break;
            case "n_plus_one":
                if (memLower.contains("n+1") || memLower.contains("n + 1") || memLower.contains("loop query") || memLower.contains("batch query") || memLower.contains("loop database")) {
                    return true;
                }
                break;
            case "try_with_resources":
                if (memLower.contains("try-with-resources") || memLower.contains("resource leak") || memLower.contains("unclosed") || memLower.contains("autocloseable")) {
                    return true;
                }
                break;
            case "null_check":
                if (memLower.contains("null check") || memLower.contains("optional") || memLower.contains("null pointer") || memLower.contains("nullpointer")) {
                    return true;
                }
                break;
            case "magic_number":
                if (memLower.contains("magic number") || memLower.contains("constant") || memLower.contains("literal value")) {
                    return true;
                }
                break;
            default:
                break;
        }

        // 3. Substring match against comment's issue text
        String issue = (comment.getIssue() != null) ? comment.getIssue().toLowerCase() : "";
        if (!issue.isEmpty()) {
            if (issue.contains("field injection") && (memLower.contains("field injection") || memLower.contains("autowired"))) {
                return true;
            }
            if (issue.contains("catch") && (memLower.contains("catch") || memLower.contains("exception"))) {
                return true;
            }
            if (issue.contains("n+1") && memLower.contains("n+1")) {
                return true;
            }
            if (issue.contains("resource") && (memLower.contains("resource") || memLower.contains("try-with-resources"))) {
                return true;
            }
        }

        return false;
    }

    private void normalizeCommentRuleKey(ReviewComment comment) {
        if (comment.getRuleKey() != null && !comment.getRuleKey().isBlank() && !comment.getRuleKey().equalsIgnoreCase("other")) {
            return;
        }
        String text = ((comment.getIssue() != null ? comment.getIssue() : "") + " "
                + (comment.getSuggestion() != null ? comment.getSuggestion() : "")).toLowerCase();

        if (text.contains("field injection") || text.contains("autowired") || text.contains("@autowired") || text.contains("constructor injection")) {
            comment.setRuleKey("field_injection");
        } else if (text.contains("empty catch") || text.contains("swallowed exception") || text.contains("catch block") || text.contains("ignore exception")) {
            comment.setRuleKey("empty_catch");
        } else if (text.contains("n+1") || text.contains("n + 1") || text.contains("loop query") || text.contains("batch query") || text.contains("database query inside a loop")) {
            comment.setRuleKey("n_plus_one");
        } else if (text.contains("try-with-resources") || text.contains("resource leak") || text.contains("unclosed") || text.contains("autocloseable") || text.contains("stream")) {
            comment.setRuleKey("try_with_resources");
        } else if (text.contains("null check") || text.contains("nullpointer") || text.contains("optional") || text.contains("null pointer")) {
            comment.setRuleKey("null_check");
        } else if (text.contains("magic number") || text.contains("constant") || text.contains("literal value")) {
            comment.setRuleKey("magic_number");
        } else {
            comment.setRuleKey("other");
        }
    }

    private String formatSkipReason(String memory, ReviewComment comment) {
        if (memory.contains("Justification / Team Convention: ")) {
            int idx = memory.indexOf("Justification / Team Convention: ");
            String note = memory.substring(idx + "Justification / Team Convention: ".length()).trim();
            if (!note.isEmpty()) {
                return "Skipped — team preference: " + note;
            }
        }
        if (memory.contains("Team Note: ")) {
            int idx = memory.indexOf("Team Note: ");
            String note = memory.substring(idx + "Team Note: ".length()).trim();
            if (!note.isEmpty()) {
                return "Skipped — team preference: " + note;
            }
        }
        if (memory.startsWith("OVERRIDDEN TEAM PREFERENCE (DO NOT FLAG)")) {
            int colonIdx = memory.indexOf("]:");
            if (colonIdx != -1) {
                String sub = memory.substring(colonIdx + 2).trim();
                int dotIdx = sub.indexOf(". Justification");
                if (dotIdx != -1) sub = sub.substring(0, dotIdx).trim();
                return "Skipped — team preference: " + sub;
            }
        }
        return "Skipped — team preference: " + memory;
    }

    /**
     * Builds a domain-aware semantic query based on code characteristics so Hindsight returns
     * precisely relevant team rules rather than noisy unrelated preferences.
     */
    public String extractSnippetCharacteristics(String code) {
        List<String> traits = new ArrayList<>();

        if (code.contains("@Autowired") || code.contains("@Inject") || code.contains("private final") || code.contains("@RequiredArgsConstructor")) {
            traits.add("dependency injection style constructor field injection autowired");
        }
        if (code.contains("catch (") || code.contains("catch(") || code.contains("throw new") || code.contains("throws ")) {
            traits.add("exception handling swallowed empty catch rethrow logging");
        }
        if (code.contains("try (") || code.contains("FileInputStream") || code.contains("BufferedReader") || code.contains("Connection") || code.contains("close()")) {
            traits.add("resource management try-with-resources streams unclosed connections");
        }
        if (code.contains("for (") || code.contains("for(") || code.contains("while") || code.contains("stream().map") || code.contains("findById") || code.contains("select ") || code.contains("repository.")) {
            traits.add("database querying n+1 loop queries batch fetch jpa");
        }
        if (code.contains("Optional") || code.contains("!= null") || code.contains("== null") || code.contains(".get(") || code.contains(".trim()")) {
            traits.add("null safety checks optional nullable validations");
        }
        if (code.matches("(?s).*(=|==|>|<)\\s*\\d{2,}.*")) {
            traits.add("magic numbers constants literal values naming");
        }

        if (traits.isEmpty()) {
            return "general code style architecture design patterns java";
        }
        return String.join(" ", traits);
    }
}
