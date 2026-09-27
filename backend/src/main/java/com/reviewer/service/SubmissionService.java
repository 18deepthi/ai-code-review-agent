package com.reviewer.service;

import com.reviewer.model.ReviewResponse;
import com.reviewer.model.SubmissionRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubmissionService {

    // Store submissions in memory per teamId
    private final Map<String, List<SubmissionRecord>> submissionsByTeam = new ConcurrentHashMap<>();

    public SubmissionRecord recordSubmission(String teamId, String codeSnippet, String language, ReviewResponse response) {
        String cleanTeamId = teamId != null && !teamId.isBlank() ? teamId.trim().toLowerCase() : "default-team";
        SubmissionRecord record = new SubmissionRecord(cleanTeamId, codeSnippet, language, response);
        submissionsByTeam.computeIfAbsent(cleanTeamId, k -> Collections.synchronizedList(new ArrayList<>())).add(0, record);
        return record;
    }

    public List<SubmissionRecord> getSubmissions(String teamId) {
        String cleanTeamId = teamId != null && !teamId.isBlank() ? teamId.trim().toLowerCase() : "default-team";
        List<SubmissionRecord> list = submissionsByTeam.getOrDefault(cleanTeamId, Collections.emptyList());
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }
}
