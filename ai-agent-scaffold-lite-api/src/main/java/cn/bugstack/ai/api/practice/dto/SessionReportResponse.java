package cn.bugstack.ai.api.practice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SessionReportResponse {
    private String sessionId;
    private String scenarioName;
    private int totalRounds;
    private int durationSeconds;
    private double averageScore;
    private List<String> allGrammarIssues;
    private List<String> vocabularySuggestions;
    private List<String> roundSummaries;
}
