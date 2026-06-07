package cn.bugstack.ai.domain.practice.service.Evaluation;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.service.IChatLlmService;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评测服务 — GPT 语法纠错 + 表达建议 + 评分
 */
@Slf4j
@Service
public class EvaluationServiceImpl implements IEvaluationService {

    private static final String EVAL_SYSTEM_PROMPT = """
            You are a professional English tutor. Evaluate the user's speech.

            Scenario: {scenarioPrompt}

            History: {history}

            Analyze the user's latest message and respond with JSON only, no markdown:
            - correctedText: grammatically corrected version
            - grammarIssues: list of grammar problems found
            - suggestions: better ways to express the same idea
            - score: overall quality from 1 to 10
            - aiReply: a natural conversational response that continues the scenario AND incorporates the correction naturally

            Example format (use this exact structure):
            {
              "correctedText": "...",
              "grammarIssues": ["...", "..."],
              "suggestions": ["...", "..."],
              "score": 7,
              "aiReply": "..."
            }
            """;

    private final IChatLlmService chatLlmService;

    public EvaluationService(IChatLlmService chatLlmService) {
        this.chatLlmService = chatLlmService;
    }

    @Override
    public EvaluationResult evaluate(String userText, Scenario scenario, String history) {
        log.info("GPT evaluate request: {}", userText);
        try {
            String systemPrompt = EVAL_SYSTEM_PROMPT
                    .replace("{scenarioPrompt}", scenario.getSystemPrompt())
                    .replace("{history}", history == null ? "" : history);
            String response = chatLlmService.chat(userText, systemPrompt);
            return parseEvaluation(response, userText);
        } catch (Exception e) {
            log.warn("Evaluation failed, falling back: {}", e.getMessage());
            return fallbackEvaluation(userText, scenario);
        }
    }

    @Override
    public EvaluationResult simpleReply(String userText, Scenario scenario) {
        try {
            String prompt = "You are " + scenario.getSystemPrompt()
                    + " Respond naturally (1-3 sentences): ";
            String reply = chatLlmService.chat(userText, prompt);
            return EvaluationResult.builder()
                    .originalText(userText).aiReply(reply).score(5).build();
        } catch (Exception e) {
            return EvaluationResult.builder()
                    .originalText(userText).aiReply("I see. Please go on.").score(5).build();
        }
    }

    private EvaluationResult fallbackEvaluation(String userText, Scenario scenario) {
        try {
            String prompt = "You are " + scenario.getSystemPrompt()
                    + " Correct the grammar and respond naturally (1-3 sentences): ";
            String reply = chatLlmService.chat(userText, prompt);
            return EvaluationResult.builder().originalText(userText)
                    .correctedText(userText).aiReply(reply).score(5).build();
        } catch (Exception e) {
            log.warn("Fallback also failed: {}", e.getMessage());
            return EvaluationResult.builder().originalText(userText)
                    .correctedText(userText).aiReply("I see. Please go on.").score(5).build();
        }
    }

    private EvaluationResult parseEvaluation(String json, String originalText) {
        try {
            String corrected = extractJsonField(json, "correctedText");
            String scoreStr = extractJsonField(json, "score");
            String aiReply = extractJsonField(json, "aiReply");
            List<String> issues = extractJsonArray(json, "grammarIssues");
            List<String> suggestions = extractJsonArray(json, "suggestions");
            int score = 5;
            try { score = Integer.parseInt(scoreStr); if (score < 1) score = 1; if (score > 10) score = 10; } catch (NumberFormatException ignored) {}
            return EvaluationResult.builder().originalText(originalText)
                    .correctedText(corrected != null ? corrected : originalText)
                    .grammarIssues(issues).suggestions(suggestions).score(score)
                    .aiReply(aiReply != null ? aiReply : "Keep going!").build();
        } catch (Exception e) {
            log.warn("Parse failed: {}", e.getMessage());
            return EvaluationResult.builder().originalText(originalText)
                    .correctedText(originalText).score(5)
                    .aiReply(json.length() > 200 ? json.substring(0, 200) : json).build();
        }
    }

    private String extractJsonField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + field.length() + 2);
        if (colon < 0) return null;
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '\\') { end += 2; continue; }
            if (json.charAt(end) == '\"') break;
            end++;
        }
        if (end >= json.length()) return null;
        return json.substring(start + 1, end).replace("\\\"", "\"").replace("\\n", "\n");
    }

    private List<String> extractJsonArray(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return List.of();
        int colon = json.indexOf(":", idx + field.length() + 2);
        if (colon < 0) return List.of();
        int start = json.indexOf("[", colon);
        if (start < 0) return List.of();
        int end = json.indexOf("]", start);
        if (end < 0) return List.of();
        String content = json.substring(start + 1, end).trim();
        if (content.isEmpty()) return List.of();
        return List.of(content.split("\\s*,\\s*"))
                .stream().map(s -> s.replaceAll("^\"|\"$", ""))
                .map(s -> s.replace("\\\"", "\"")).toList();
    }
}

