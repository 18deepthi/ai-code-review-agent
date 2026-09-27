package com.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppProperties;
import com.reviewer.model.FeedbackRequest;
import com.reviewer.model.FeedbackResponse;
import com.reviewer.model.ReviewComment;
import com.reviewer.model.ReviewResponse;
import com.reviewer.service.CodeReviewService;
import com.reviewer.service.FeedbackService;
import com.reviewer.service.GroqClient;
import com.reviewer.service.HindsightClient;
import com.reviewer.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionAndDeduplicationTest {

    private HindsightClient hindsightClient;
    private GroqClient groqClient;
    private SubmissionService submissionService;
    private CodeReviewService codeReviewService;
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getHindsight().setApiKey(""); // use local store for clean test
        ObjectMapper mapper = new ObjectMapper();
        hindsightClient = new HindsightClient(properties, mapper);
        groqClient = new GroqClient(properties, mapper);
        submissionService = new SubmissionService();
        codeReviewService = new CodeReviewService(hindsightClient, groqClient, submissionService);
        feedbackService = new FeedbackService(hindsightClient);
    }

    @Test
    @DisplayName("Matching Logic: Suppresses field injection when recalled memory permits it")
    void testMatchingLogicSuppression() {
        List<ReviewComment> comments = new ArrayList<>();

        ReviewComment comment1 = new ReviewComment();
        comment1.setRuleKey("field_injection");
        comment1.setIssue("Field injection with @Autowired on private field");
        comment1.setSuggestion("Use constructor injection");
        comment1.setSkippedDueToMemory(false);
        comments.add(comment1);

        ReviewComment comment2 = new ReviewComment();
        comment2.setRuleKey("other");
        comment2.setIssue("Missing import for OrderRepository");
        comment2.setSkippedDueToMemory(false);
        comments.add(comment2);

        List<String> recalledMemories = List.of("field injection permitted for team-alpha");

        // Execute matching logic
        codeReviewService.applyMemoryOverrides(comments, recalledMemories);

        ReviewResponse response = new ReviewResponse("team-alpha", comments, recalledMemories);

        assertTrue(comment1.isSkippedDueToMemory(), "Field injection comment must be marked as skipped");
        assertTrue(comment1.getMemoryReason().contains("team preference"), "Memory reason must be set");
        assertFalse(comment2.isSkippedDueToMemory(), "Unrelated import comment must remain actionable");
        assertEquals(1, response.getSkippedCount(), "Skipped count must be exactly 1");
        assertEquals(1, response.getActiveIssuesCount(), "Active issues count must be exactly 1");
    }

    @Test
    @DisplayName("Deduplication: Rejecting the same issue twice for the same team does not create duplicates")
    void testDeduplication() {
        String teamId = "team-dedup-test";

        FeedbackRequest req1 = new FeedbackRequest();
        req1.setTeamId(teamId);
        req1.setDecision("rejected");
        req1.setRule("Do not flag field injection");
        req1.setRuleKey("field_injection");
        req1.setNote("Permitted in legacy services");

        FeedbackResponse resp1 = feedbackService.processFeedback(req1);
        assertTrue(resp1.isSuccess());
        assertEquals(1, hindsightClient.listMemories(teamId).size(), "Should have exactly 1 memory stored");

        // Second identical feedback request
        FeedbackRequest req2 = new FeedbackRequest();
        req2.setTeamId(teamId);
        req2.setDecision("rejected");
        req2.setRule("Field injection with @Autowired on private field");
        req2.setRuleKey("field_injection");
        req2.setNote("Permitted in legacy services");

        FeedbackResponse resp2 = feedbackService.processFeedback(req2);
        assertTrue(resp2.isSuccess());
        assertTrue(resp2.getMessage().contains("deduplicated"), "Response message should reflect deduplication");

        // Verify still exactly 1 memory
        assertEquals(1, hindsightClient.listMemories(teamId).size(), "Must not create duplicate memory for same team and issue type");
    }
}
