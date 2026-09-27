package com.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppProperties;
import com.reviewer.model.ReviewComment;
import com.reviewer.service.GroqClient;
import com.reviewer.service.HindsightClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IsolatedClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppProperties appProperties = new AppProperties();

    @Test
    @DisplayName("HindsightClient: write and recall works in isolation")
    void testHindsightWriteAndRecall() {
        HindsightClient client = new HindsightClient(appProperties, objectMapper);
        String teamId = "test-team-alpha";
        String rule = "Never flag field injection in legacy controller packages";

        boolean writeResult = client.writeMemory(teamId, rule, Map.of("decision", "rejected", "ruleKey", "field_injection"));
        assertTrue(writeResult, "writeMemory should return true");

        List<String> recalled = client.recallMemory(teamId, "dependency injection controller");
        assertNotNull(recalled, "Recalled list should not be null");
        assertFalse(recalled.isEmpty(), "Recalled list should contain written rule");
        assertTrue(recalled.get(0).toLowerCase().contains("field injection"), "Recalled memory should match pattern");
    }

    @Test
    @DisplayName("GroqClient: review generation and skipped preference logic works in isolation")
    void testGroqReviewWithPreference() {
        GroqClient client = new GroqClient(appProperties, objectMapper);
        String code = """
                public class UserService {
                    @Autowired
                    private UserRepository userRepository;
                }
                """;

        // First without memory: should flag field injection
        List<ReviewComment> commentsBefore = client.reviewCode(code, List.of());
        assertNotNull(commentsBefore);
        boolean flaggedFieldInjection = commentsBefore.stream()
                .anyMatch(c -> c.getIssue().toLowerCase().contains("field injection") && !c.isSkippedDueToMemory());
        assertTrue(flaggedFieldInjection, "Should flag field injection when no preference is present");

        // Second with memory: should skip known preference
        List<ReviewComment> commentsAfter = client.reviewCode(code, List.of("RULE / OVERRIDDEN PREFERENCE: Field injection is accepted for legacy services"));
        assertNotNull(commentsAfter);
        boolean skippedFieldInjection = commentsAfter.stream()
                .anyMatch(c -> c.isSkippedDueToMemory() && c.getMemoryReason().contains("Field injection"));
        assertTrue(skippedFieldInjection, "Should mark field injection as skippedDueToMemory when preference is present");
    }
}
