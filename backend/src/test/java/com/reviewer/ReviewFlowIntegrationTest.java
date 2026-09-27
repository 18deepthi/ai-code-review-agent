package com.reviewer;

import com.reviewer.model.*;
import com.reviewer.service.CodeReviewService;
import com.reviewer.service.FeedbackService;
import com.reviewer.service.HindsightClient;
import com.reviewer.service.SubmissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReviewFlowIntegrationTest {

    @Autowired
    private CodeReviewService codeReviewService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private HindsightClient hindsightClient;

    @Autowired
    private SubmissionService submissionService;

    @Test
    @DisplayName("End-to-End Learning Progression: Flag -> Reject -> Store Memory -> Re-review Skips Issue")
    void testEndToEndReviewLearningProgression() {
        String teamId = "team-gamma-" + System.currentTimeMillis();
        String code = """
                public class OrderService {
                    @Autowired
                    private OrderRepository orderRepository;

                    public void process(Long id) {
                        try {
                            orderRepository.deleteById(id);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
                """;

        // Step 1: Initial Review (Cold start, no memories stored)
        ReviewRequest request1 = new ReviewRequest(teamId, code, "java");
        ReviewResponse response1 = codeReviewService.performReview(request1);

        assertNotNull(response1);
        assertNotNull(response1.getComments());
        assertTrue(response1.getTotalIssuesCount() >= 1, "Initial review should identify code issues");

        ReviewComment fieldInjComment = response1.getComments().stream()
                .filter(c -> "field_injection".equals(c.getRuleKey()) || c.getIssue().toLowerCase().contains("field injection"))
                .findFirst()
                .orElse(null);

        assertNotNull(fieldInjComment, "Field injection issue should be flagged initially");
        assertFalse(fieldInjComment.isSkippedDueToMemory(), "Field injection should not be skipped on first run");

        // Step 2: Human Feedback - Reject field injection recommendation
        FeedbackRequest feedback = new FeedbackRequest();
        feedback.setTeamId(teamId);
        feedback.setReviewId(response1.getReviewId());
        feedback.setCommentId(fieldInjComment.getId());
        feedback.setDecision("rejected");
        feedback.setRule("Field injection (@Autowired) is accepted for legacy order management services");
        feedback.setNote("Approved architectural waiver by team lead");
        feedback.setSourceSnippetExcerpt("@Autowired private OrderRepository orderRepository;");

        FeedbackResponse feedbackResponse = feedbackService.processFeedback(feedback);
        assertNotNull(feedbackResponse);
        assertTrue(feedbackResponse.isSuccess(), "Feedback persistence should succeed");

        // Step 3: Verify Memory Dashboard endpoint receives the newly stored memory
        List<MemoryRecord> memories = hindsightClient.listMemories(teamId);
        assertFalse(memories.isEmpty(), "Memory dashboard should list the retained team preference");
        assertTrue(memories.stream().anyMatch(m -> m.getRule().contains("Field injection")), "Memory list should contain the rejected rule");

        // Step 4: Second Review (Warm start, memory recall active)
        ReviewRequest request2 = new ReviewRequest(teamId, code, "java");
        ReviewResponse response2 = codeReviewService.performReview(request2);

        assertNotNull(response2);
        assertFalse(response2.getRecalledMemories().isEmpty(), "Second review must recall the team preference from Hindsight");

        ReviewComment fieldInjComment2 = response2.getComments().stream()
                .filter(c -> "field_injection".equals(c.getRuleKey()) || c.getIssue().toLowerCase().contains("field injection"))
                .findFirst()
                .orElse(null);

        assertNotNull(fieldInjComment2, "Field injection should be present in response");
        assertTrue(fieldInjComment2.isSkippedDueToMemory(), "Field injection MUST be skipped due to remembered team preference");
        assertTrue(fieldInjComment2.getMemoryReason().contains("Field injection") || fieldInjComment2.getMemoryReason().contains("team preference"),
                "Memory reason should be populated clearly for UI display");

        // Step 5: Verify Submissions history
        List<SubmissionRecord> submissions = submissionService.getSubmissions(teamId);
        assertEquals(2, submissions.size(), "Two submissions should be recorded");
        assertTrue(submissions.get(0).getSkippedCount() > 0, "Latest submission should have skipped count recorded");
    }
}
