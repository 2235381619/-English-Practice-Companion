package cn.bugstack.ai.domain.practice.model.entity;

import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.model.valobj.SessionReport;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 口语练习会话实体
 */
@Getter
public class PracticeSession {

    private final String sessionId;
    private final Scenario scenario;
    private final long createdTime;
    private volatile boolean active;
    private final List<ConversationRound> rounds;

    public PracticeSession(Scenario scenario) {
        this.sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        this.scenario = scenario;
        this.createdTime = System.currentTimeMillis();
        this.active = true;
        this.rounds = new ArrayList<>();
    }

    public ConversationRound addRound(String userInput) {
        ConversationRound round = new ConversationRound(rounds.size() + 1, userInput);
        rounds.add(round);
        return round;
    }

    public ConversationRound currentRound() {
        if (rounds.isEmpty()) return null;
        return rounds.get(rounds.size() - 1);
    }

    public int roundCount() {
        return rounds.size();
    }

    public void close() {
        this.active = false;
    }

    public SessionReport generateReport() {
        List<String> allIssues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        double totalScore = 0;

        for (ConversationRound r : rounds) {
            if (r.getEvaluation() != null) {
                var eval = r.getEvaluation();
                if (eval.getGrammarIssues() != null) allIssues.addAll(eval.getGrammarIssues());
                if (eval.getSuggestions() != null) suggestions.addAll(eval.getSuggestions());
                totalScore += eval.getScore();
                summaries.add("Round " + r.getRoundNumber() + ": score=" + eval.getScore());
            }
        }

        return SessionReport.builder()
                .sessionId(sessionId)
                .scenario(scenario)
                .totalRounds(rounds.size())
                .durationSeconds((int) ((System.currentTimeMillis() - createdTime) / 1000))
                .averageScore(rounds.isEmpty() ? 0 : totalScore / rounds.size())
                .allGrammarIssues(allIssues)
                .vocabularySuggestions(suggestions.stream().distinct().toList())
                .roundSummaries(summaries)
                .build();
    }
}
