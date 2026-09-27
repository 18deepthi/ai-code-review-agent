package com.reviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppProperties;
import com.reviewer.model.MemoryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * HindsightClient: Interacts with Hindsight Cloud REST API (hindsight.vectorize.io).
 * Wraps writeMemory, recallMemory, and listMemories with graceful resilience and local fallback.
 */
@Service
public class HindsightClient {

    private static final Logger log = LoggerFactory.getLogger(HindsightClient.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // In-memory fallback and synchronization store per teamId
    private final Map<String, List<MemoryRecord>> localMemoryStore = new ConcurrentHashMap<>();

    public HindsightClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(appProperties.getHindsight().getTimeoutMs()))
                .build();
    }

    /**
     * Retains a memory into Hindsight for a specific team bank.
     */
    public boolean writeMemory(String teamId, String content, Map<String, String> metadata) {
        String cleanTeamId = sanitizeBankId(teamId);
        log.info("Writing memory to Hindsight for bank: '{}' - content: {}", cleanTeamId, content);

        // 1. Always keep local fallback store updated
        MemoryRecord record = new MemoryRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTeamId(cleanTeamId);
        record.setRule(content);
        record.setDecision(metadata != null ? metadata.getOrDefault("decision", "accepted") : "accepted");
        record.setSourceSnippetExcerpt(metadata != null ? metadata.getOrDefault("sourceSnippetExcerpt", "") : "");
        record.setNote(metadata != null ? metadata.getOrDefault("note", "") : "");
        record.setTimestamp(Instant.now());
        Map<String, String> scopedMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        scopedMetadata.put("teamId", cleanTeamId);
        scopedMetadata.put("team_id", cleanTeamId);
        scopedMetadata.put("user_id", cleanTeamId);
        scopedMetadata.put("scope", "team:" + cleanTeamId);
        record.setMetadata(scopedMetadata);

        localMemoryStore.computeIfAbsent(cleanTeamId, k -> Collections.synchronizedList(new ArrayList<>())).add(0, record);

        // 2. Call Hindsight Cloud REST API if API key is present
        String apiKey = appProperties.getHindsight().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("No Hindsight API key configured. Preserved memory in resilient local bank for '{}'.", cleanTeamId);
            return true;
        }

        try {
            String baseUrl = appProperties.getHindsight().getBaseUrl();
            String url = baseUrl + "/v1/default/banks/" + cleanTeamId + "/memories";

            Map<String, Object> memoryItem = new HashMap<>();
            memoryItem.put("content", content);
            memoryItem.put("metadata", scopedMetadata);
            memoryItem.put("tags", List.of("team:" + cleanTeamId, cleanTeamId));

            Map<String, Object> payload = new HashMap<>();
            payload.put("items", List.of(memoryItem));
            payload.put("async", false);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(appProperties.getHindsight().getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Hindsight writeMemory succeeded for bank '{}' with status {}", cleanTeamId, response.statusCode());
                return true;
            } else {
                log.warn("Hindsight writeMemory returned status {}: {}. Kept in local fallback store.",
                        response.statusCode(), response.body());
                return true;
            }
        } catch (Exception e) {
            log.warn("Error calling Hindsight Cloud API for writeMemory: {}. Gracefully falling back to local memory store.",
                    e.getMessage());
            return true;
        }
    }

    /**
     * Recalls relevant memories from Hindsight based on a semantic/keyword query and strict team scope.
     */
    public List<String> recallMemory(String teamId, String query) {
        String cleanTeamId = sanitizeBankId(teamId);
        log.info("Recalling memories from Hindsight for bank: '{}' with query: '{}'", cleanTeamId, query);

        List<String> results = new ArrayList<>();
        String apiKey = appProperties.getHindsight().getApiKey();

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String baseUrl = appProperties.getHindsight().getBaseUrl();
                String url = baseUrl + "/v1/default/banks/" + cleanTeamId + "/memories/recall";

                Map<String, Object> payload = new HashMap<>();
                payload.put("query", query);
                // Strict tag-based scoping to ensure zero cross-team contamination
                payload.put("tags", List.of("team:" + cleanTeamId));
                payload.put("tagsMatch", "all_strict");

                payload.put("budget", "mid");

                String requestBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + apiKey.trim())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMillis(appProperties.getHindsight().getTimeoutMs()))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode resultsNode = root.get("results");
                    if (resultsNode == null && root.isArray()) {
                        resultsNode = root;
                    }
                    if (resultsNode != null && resultsNode.isArray()) {
                        for (JsonNode item : resultsNode) {
                            // Verify team scope if metadata or tags present
                            if (item.has("metadata")) {
                                JsonNode meta = item.get("metadata");
                                String itemTeam = meta.has("teamId") ? meta.get("teamId").asText() :
                                        (meta.has("team_id") ? meta.get("team_id").asText() : "");
                                if (!itemTeam.isEmpty() && !itemTeam.equalsIgnoreCase(cleanTeamId)) {
                                    continue; // Skip memory belonging to a different team
                                }
                            }

                            String text = item.has("text") ? item.get("text").asText() : "";
                            if (text.isEmpty() && item.has("content")) {
                                text = item.get("content").asText();
                            }
                            if (!text.isEmpty()) {
                                results.add(text);
                            }
                        }
                    }
                    log.info("Recalled {} memories from Hindsight Cloud for bank '{}'", results.size(), cleanTeamId);
                } else {
                    log.warn("Hindsight recall returned status {}: {}. Falling back to local store.",
                            response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.warn("Error calling Hindsight Cloud API for recallMemory: {}. Falling back to local store.",
                        e.getMessage());
            }
        }

        // Only fall back to local store if Hindsight Cloud is not configured or returned no memories
        if (results.isEmpty()) {
            List<MemoryRecord> localRecords = localMemoryStore.getOrDefault(cleanTeamId, Collections.emptyList());
            synchronized (localRecords) {
                for (MemoryRecord record : localRecords) {
                    if (record.getTeamId() != null && !record.getTeamId().equalsIgnoreCase(cleanTeamId)) {
                        continue; // Strict isolation safeguard
                    }
                    String ruleText = formatRecordSummary(record);
                    if (!results.contains(ruleText) && isRelevant(query, record)) {
                        results.add(ruleText);
                    }
                }
            }
        }

        return results;
    }

    public void clearMemories(String teamId) {
        String cleanTeamId = sanitizeBankId(teamId);
        localMemoryStore.remove(cleanTeamId);
        log.info("Cleared local memories for bank '{}'", cleanTeamId);

        String apiKey = appProperties.getHindsight().getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String baseUrl = appProperties.getHindsight().getBaseUrl();
                String url = baseUrl + "/v1/default/banks/" + cleanTeamId;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + apiKey.trim())
                        .timeout(Duration.ofMillis(appProperties.getHindsight().getTimeoutMs()))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Hindsight Cloud bank '{}' delete returned status: {}", cleanTeamId, response.statusCode());
            } catch (Exception e) {
                log.warn("Error calling Hindsight Cloud DELETE bank for '{}': {}", cleanTeamId, e.getMessage());
            }
        }
    }

    /**
     * Lists all stored memories for a team (for the Memory Dashboard).
     */
    public List<MemoryRecord> listMemories(String teamId) {
        String cleanTeamId = sanitizeBankId(teamId);
        List<MemoryRecord> records = new ArrayList<>();

        // If Cloud API configured, fetch from Cloud first
        String apiKey = appProperties.getHindsight().getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                List<String> cloudMemories = recallMemory(cleanTeamId, "team code review preference rule decision convention");
                for (String mem : cloudMemories) {
                    String ruleKey = FeedbackService.deriveRuleKey(null, mem, "", "");
                    boolean alreadyExists = records.stream().anyMatch(r -> {
                        String rKey = FeedbackService.deriveRuleKey(null, r.getRule(), "", "");
                        return (!ruleKey.equals("general_rule") && ruleKey.equalsIgnoreCase(rKey))
                                || r.getRule().equalsIgnoreCase(mem)
                                || mem.contains(r.getRule())
                                || r.getRule().contains(mem);
                    });
                    if (!alreadyExists) {
                        MemoryRecord cloudRec = new MemoryRecord();
                        cloudRec.setId(UUID.randomUUID().toString());
                        cloudRec.setTeamId(cleanTeamId);
                        cloudRec.setRule(mem);
                        boolean isRejected = mem.toLowerCase().contains("rejected")
                                || mem.toLowerCase().contains("overridden")
                                || mem.toLowerCase().contains("do not flag")
                                || mem.toLowerCase().contains("permitted")
                                || mem.toLowerCase().contains("allowed")
                                || mem.toLowerCase().contains("preference");
                        cloudRec.setDecision(isRejected ? "rejected" : "accepted");
                        cloudRec.setTimestamp(Instant.now());
                        records.add(cloudRec);
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching cloud memories list: {}", e.getMessage());
            }
        }

        // If no cloud memories (or no cloud key configured), use local store
        if (records.isEmpty()) {
            List<MemoryRecord> local = localMemoryStore.getOrDefault(cleanTeamId, Collections.emptyList());
            synchronized (local) {
                records.addAll(local);
            }
        }

        return records;
    }

    private boolean isRelevant(String query, MemoryRecord record) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase();
        String rule = (record.getRule() != null ? record.getRule() : "").toLowerCase();
        String excerpt = (record.getSourceSnippetExcerpt() != null ? record.getSourceSnippetExcerpt() : "").toLowerCase();
        String note = (record.getNote() != null ? record.getNote() : "").toLowerCase();

        // Break query into keywords
        String[] keywords = q.split("[^a-zA-Z0-9]+");
        for (String kw : keywords) {
            if (kw.length() > 3 && (rule.contains(kw) || excerpt.contains(kw) || note.contains(kw))) {
                return true;
            }
        }
        // If query has less than 2 distinct keywords, return true
        return keywords.length <= 1;
    }

    private String formatRecordSummary(MemoryRecord record) {
        String decisionText = "rejected".equalsIgnoreCase(record.getDecision()) ? "RULE / OVERRIDDEN PREFERENCE: " : "ACCEPTED PRACTICE: ";
        StringBuilder sb = new StringBuilder();
        sb.append(decisionText).append(record.getRule());
        if (record.getNote() != null && !record.getNote().isBlank()) {
            sb.append(" (Team Note: ").append(record.getNote()).append(")");
        }
        return sb.toString();
    }

    private String sanitizeBankId(String teamId) {
        if (teamId == null || teamId.trim().isEmpty()) {
            return "default-team";
        }
        // Hindsight bank IDs allow alphanumeric, dashes, and underscores
        return teamId.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "-");
    }
}
