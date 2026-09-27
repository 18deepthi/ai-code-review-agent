package com.reviewer;

import com.reviewer.model.FeedbackRequest;
import com.reviewer.model.ReviewComment;
import com.reviewer.model.ReviewRequest;
import com.reviewer.model.ReviewResponse;
import com.reviewer.service.CodeReviewService;
import com.reviewer.service.FeedbackService;
import com.reviewer.service.HindsightClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TeamIsolationTest {

    @Autowired
    private CodeReviewService codeReviewService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private HindsightClient hindsightClient;

    @Test
    @DisplayName("Verify strict memory isolation: Team A rejection must NOT affect Team B")
    void testStrictMemoryIsolationBetweenTeams() {
        String teamAlpha = "team-alpha-" + System.currentTimeMillis();
        String teamBackendCore = "team-backend-core-" + System.currentTimeMillis();

        String codeSnippet = """
                package com.example.service;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;

                @Service
                public class OrderService {
                    @Autowired
                    private OrderRepository orderRepository;
                }
                """;

        // Step 1: Team Alpha reviews code (Baseline -> flags field injection)
        ReviewResponse alphaRev1 = codeReviewService.performReview(new ReviewRequest(teamAlpha, codeSnippet, "java"));
        assertEquals(1, alphaRev1.getTotalIssuesCount());
        assertEquals(0, alphaRev1.getSkippedCount());
        assertEquals(1, alphaRev1.getActiveIssuesCount());
        assertFalse(alphaRev1.getComments().get(0).isSkippedDueToMemory());

        // Step 2: Team Alpha rejects the field injection recommendation
        FeedbackRequest alphaFeedback = new FeedbackRequest();
        alphaFeedback.setTeamId(teamAlpha);
        alphaFeedback.setReviewId(alphaRev1.getReviewId());
        alphaFeedback.setCommentId(alphaRev1.getComments().get(0).getId());
        alphaFeedback.setDecision("rejected");
        alphaFeedback.setRule("Field injection (@Autowired) is accepted for legacy services");
        alphaFeedback.setNote("Architectural exemption granted for team-alpha");
        alphaFeedback.setSourceSnippetExcerpt("@Autowired private OrderRepository orderRepository;");
        feedbackService.processFeedback(alphaFeedback);

        // Step 3: Team Alpha reviews again -> Field injection MUST be SKIPPED due to Team Alpha's memory
        ReviewResponse alphaRev2 = codeReviewService.performReview(new ReviewRequest(teamAlpha, codeSnippet, "java"));
        assertEquals(1, alphaRev2.getTotalIssuesCount());
        assertEquals(1, alphaRev2.getSkippedCount(), "Team Alpha MUST skip field injection due to learned memory");
        assertEquals(0, alphaRev2.getActiveIssuesCount());
        assertTrue(alphaRev2.getComments().get(0).isSkippedDueToMemory());
        assertTrue(alphaRev2.getComments().get(0).getMemoryReason().contains("Field injection"));

        // Step 4: Team Backend Core reviews the EXACT SAME code snippet!
        // MUST NOT skip! Team Backend Core never gave feedback!
        ReviewResponse backendCoreRev = codeReviewService.performReview(new ReviewRequest(teamBackendCore, codeSnippet, "java"));
        assertNotNull(backendCoreRev);
        assertEquals(0, backendCoreRev.getRecalledMemories().size(),
                "Team Backend Core must have 0 recalled memories (zero leakage from Team Alpha)");
        assertEquals(1, backendCoreRev.getTotalIssuesCount(), "Team Backend Core must have 1 total issue");
        assertEquals(0, backendCoreRev.getSkippedCount(), "Team Backend Core must have 0 skipped issues");
        assertEquals(1, backendCoreRev.getActiveIssuesCount(), "Team Backend Core must have 1 active issue");

        ReviewComment comment = backendCoreRev.getComments().get(0);
        assertFalse(comment.isSkippedDueToMemory(),
                "Field injection must be flagged fresh for Team Backend Core, NOT skipped!");
    }
}
