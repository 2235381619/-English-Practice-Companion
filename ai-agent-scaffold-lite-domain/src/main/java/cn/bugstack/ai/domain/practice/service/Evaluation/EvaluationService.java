package cn.bugstack.ai.domain.practice.service.Evaluation;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 评测服务 — GPT 语法纠错 + 表达建议 + 评分
 *
 * 纯功能实现，属于 domain 层。
 */
@Slf4j
@Service
public class EvaluationService implements IEvaluationService {

    private static final String EVAL_TEMPLATE = """
            You are an English tutor. Evaluate the user's speech in this scenario:
            Scenario: {scenarioPrompt}

            User said: {userText}
            History: {history}

            Respond in this JSON format (no markdown):
            {
              "correctedText": "Grammatically corrected version of the user text",
              "grammarIssues": ["issue 1", "issue 2"],
              "suggestions": ["better expression 1", "better expression 2"],
              "score": <1-10>,
              "aiReply": "Natural conversational response continuing the scenario AND incorporating the correction"
            }
            """;

    private static final String SIMPLE_EVAL_TEMPLATE = """
            You are a {scenarioPrompt}

            User said: {userText}

            Respond naturally — continue the conversation, correct grammar subtly, and suggest better expressions naturally.
            Keep it concise (1-3 sentences).
            """;

    private final ChatModel chatModel;

    public EvaluationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public EvaluationResult evaluate(String userText, Scenario scenario, String history) {
        log.info("GPT evaluate request: {}", userText);
        try {
            Map<String, Object> vars = Map.of(
                    "scenarioPrompt", scenario.getSystemPrompt(),
                    "userText", userText,
                    "history", history == null ? "" : history
            );
            PromptTemplate template = new PromptTemplate(EVAL_TEMPLATE);
            Prompt prompt = template.create(vars);
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            return parseEvaluation(response, userText);
        } catch (Exception e) {
            log.warn("Evaluation failed, falling back: {}", e.getMessage());
            return fallbackEvaluation(userText, scenario);
        }
    }

    public EvaluationResult simpleReply(String userText, Scenario scenario) {
        return fallbackEvaluation(userText, scenario);
    }

    private EvaluationResult fallbackEvaluation(String userText, Scenario scenario) {
        try {
            Map<String, Object> vars = Map.of(
                    "scenarioPrompt", scenario.getSystemPrompt(),
                    "userText", userText
            );
            PromptTemplate template = new PromptTemplate(SIMPLE_EVAL_TEMPLATE);
            Prompt prompt = template.create(vars);
            String reply = chatModel.call(prompt).getResult().getOutput().getText();
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
