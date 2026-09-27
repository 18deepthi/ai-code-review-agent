package com.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewRequest {
    private String teamId;
    private String codeSnippet;
    private String language = "java";

    public ReviewRequest() {}

    public ReviewRequest(String teamId, String codeSnippet, String language) {
        this.teamId = teamId;
        this.codeSnippet = codeSnippet;
        this.language = language;
    }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
