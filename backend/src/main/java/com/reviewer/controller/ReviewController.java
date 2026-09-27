package com.reviewer.controller;

import com.reviewer.model.*;
import com.reviewer.service.CodeReviewService;
import com.reviewer.service.FeedbackService;
import com.reviewer.service.HindsightClient;
import com.reviewer.service.SubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final CodeReviewService codeReviewService;
    private final FeedbackService feedbackService;
    private final HindsightClient hindsightClient;
    private final SubmissionService submissionService;

    public ReviewController(CodeReviewService codeReviewService,
                            FeedbackService feedbackService,
                            HindsightClient hindsightClient,
                            SubmissionService submissionService) {
        this.codeReviewService = codeReviewService;
        this.feedbackService = feedbackService;
        this.hindsightClient = hindsightClient;
        this.submissionService = submissionService;
    }

    /**
     * POST /api/review
     * Recalls Hindsight memories for teamId -> builds prompt -> calls Groq -> returns structured review comments.
     */
    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> reviewCode(@RequestBody ReviewRequest request) {
        log.info("Received review request for team: {}", request.getTeamId());
        ReviewResponse response = codeReviewService.performReview(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/feedback
     * Writes decision & rule back to Hindsight as a new memory.
     */
    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(@RequestBody FeedbackRequest request) {
        log.info("Received feedback for reviewId: {}, commentId: {}, decision: {}",
                request.getReviewId(), request.getCommentId(), request.getDecision());
        FeedbackResponse response = feedbackService.processFeedback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/memories/{teamId}
     * Returns all stored memories for that team, powering the live memory dashboard.
     */
    @GetMapping("/memories/{teamId}")
    public ResponseEntity<List<MemoryRecord>> getMemories(@PathVariable String teamId) {
        log.info("Fetching stored memories for team: {}", teamId);
        List<MemoryRecord> memories = hindsightClient.listMemories(teamId);
        return ResponseEntity.ok(memories);
    }

    /**
     * DELETE /api/memories/{teamId}
     * Clears memories for a specific team bank.
     */
    @DeleteMapping("/memories/{teamId}")
    public ResponseEntity<Void> clearMemories(@PathVariable String teamId) {
        log.info("Clearing stored memories for team: {}", teamId);
        hindsightClient.clearMemories(teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/submissions/{teamId}
     * Returns review history for the team (for the History screen).
     */
    @GetMapping("/submissions/{teamId}")
    public ResponseEntity<List<SubmissionRecord>> getSubmissions(@PathVariable String teamId) {
        log.info("Fetching review history for team: {}", teamId);
        List<SubmissionRecord> submissions = submissionService.getSubmissions(teamId);
        return ResponseEntity.ok(submissions);
    }
}
