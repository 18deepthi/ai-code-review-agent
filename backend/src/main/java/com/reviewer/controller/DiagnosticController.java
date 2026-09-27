package com.reviewer.controller;

import com.reviewer.config.AppProperties;
import com.reviewer.model.ReviewComment;
import com.reviewer.service.GroqClient;
import com.reviewer.service.HindsightClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    private final GroqClient groqClient;
    private final HindsightClient hindsightClient;
    private final AppProperties appProperties;

    public DiagnosticController(GroqClient groqClient, HindsightClient hindsightClient, AppProperties appProperties) {
        this.groqClient = groqClient;
        this.hindsightClient = hindsightClient;
        this.appProperties = appProperties;
    }

    /**
     * Checks health and configuration of Groq and Hindsight services.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        String groqKey = appProperties.getGroq().getApiKey();
        boolean groqConfigured = groqKey != null && !groqKey.trim().isEmpty() && !groqKey.contains("your_groq");
        health.put("groqConfigured", groqConfigured);
        health.put("groqModel", appProperties.getGroq().getModel());
        health.put("groqFallbackModel", appProperties.getGroq().getFallbackModel());

        String hindsightKey = appProperties.getHindsight().getApiKey();
        boolean hindsightConfigured = hindsightKey != null && !hindsightKey.trim().isEmpty() && !hindsightKey.contains("your_hindsight");
        health.put("hindsightConfigured", hindsightConfigured);
        health.put("hindsightBaseUrl", appProperties.getHindsight().getBaseUrl());

        return ResponseEntity.ok(health);
    }

    /**
     * Diagnostic endpoint: Test Hindsight write and recall in isolation.
     */
    @PostMapping("/test-hindsight")
    public ResponseEntity<Map<String, Object>> testHindsight(
            @RequestParam(defaultValue = "team-test-bank") String teamId,
            @RequestParam(defaultValue = "Constructor injection is preferred over field injection in all services") String content) {

        Map<String, Object> response = new HashMap<>();
        response.put("teamId", teamId);
        response.put("testedContent", content);

        // 1. Write memory
        boolean writeSuccess = hindsightClient.writeMemory(teamId, content, Map.of(
                "category", "architecture",
                "source", "diagnostic-test",
                "decision", "accepted"
        ));
        response.put("writeSuccess", writeSuccess);

        // 2. Recall memory
        List<String> recalled = hindsightClient.recallMemory(teamId, "injection dependencies spring");
        response.put("recalledMemories", recalled);
        response.put("recalledCount", recalled.size());
        response.put("verified", !recalled.isEmpty());

        return ResponseEntity.ok(response);
    }

    /**
     * Diagnostic endpoint: Test Groq review call in isolation.
     */
    @PostMapping("/test-groq")
    public ResponseEntity<Map<String, Object>> testGroq(@RequestBody(required = false) Map<String, String> body) {
        String code = body != null && body.containsKey("codeSnippet")
                ? body.get("codeSnippet")
                : "public class SampleService { @Autowired private UserRepository repo; }";

        List<String> mockMemories = body != null && body.containsKey("preference")
                ? List.of(body.get("preference"))
                : List.of("Do not flag field injection with @Autowired");

        Map<String, Object> result = new HashMap<>();
        result.put("sampleCode", code);
        result.put("injectedMemories", mockMemories);

        List<ReviewComment> comments = groqClient.reviewCode(code, mockMemories);
        result.put("comments", comments);
        result.put("commentsCount", comments.size());

        return ResponseEntity.ok(result);
    }
}
