package cn.bugstack.ai.domain.practice.service.Evaluation;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EvaluationServiceImpl implements IEvaluationService {

    private static final String EVAL_SYSTEM_PROMPT = """
        You are a professional English tutor. Evaluate the user's latest speech.
        Scenario: {scenarioPrompt}

        Respond with valid JSON only, no markdown.
        Example:
        {
          "correctedText": "I went to the store yesterday.",
          "grammarIssues": ["Wrong verb tense"],
          "suggestions": ["Use past tense for completed actions"],
          "score": 7,
          "aiReply": "Good try! Remember to use past tense."
        }
        """;

    @Resource(name = "evlChatModel")
    private ChatModel evalChatModel;

    @Resource(name = "evalChatMemory")
    private ChatMemory chatMemory;

    @Override
    public EvaluationResult evaluate(String sessionId, String userText, Scenario scenario) {
        log.info("GPT evaluate request: {}", userText);
        try {
            String systemText = EVAL_SYSTEM_PROMPT.replace("{scenarioPrompt}", scenario.getSystemPrompt());
            chatMemory.add(sessionId, new UserMessage(userText));
            List<Message> messages = new ArrayList<>(chatMemory.get(sessionId).size() + 1);
            messages.add(new SystemMessage(systemText));
            messages.addAll(chatMemory.get(sessionId));
            Prompt prompt = new Prompt(messages);
            String response = evalChatModel.call(prompt).getResult().getOutput().getText();
            chatMemory.add(sessionId, new AssistantMessage(response));
            return parseEvaluation(response, userText);
        } catch (Exception e) {
            log.warn("Evaluation failed, falling back: {}", e.getMessage());
            return fallbackEvaluation(userText, scenario);
        }
    }

    @Override
    public EvaluationResult simpleReply(String userText, Scenario scenario) {
        try {
            List<Message> msgs = List.of(new SystemMessage("You are " + scenario.getSystemPrompt()), new UserMessage(userText));
            String reply = evalChatModel.call(new Prompt(msgs)).getResult().getOutput().getText();
            return EvaluationResult.builder().originalText(userText).aiReply(reply).score(5).build();
        } catch (Exception e) {
            return EvaluationResult.builder().originalText(userText).aiReply("I see. Please go on.").score(5).build();
        }
    }

    private EvaluationResult fallbackEvaluation(String userText, Scenario scenario) {
        try {
            List<Message> msgs = List.of(new SystemMessage("You are " + scenario.getSystemPrompt()), new UserMessage(userText));
            String reply = evalChatModel.call(new Prompt(msgs)).getResult().getOutput().getText();
            return EvaluationResult.builder().originalText(userText).correctedText(userText).aiReply(reply).score(5).build();
        } catch (Exception e) {
            log.warn("Fallback also failed: {}", e.getMessage());
            return EvaluationResult.builder().originalText(userText).correctedText(userText).aiReply("I see. Please go on.").score(5).build();
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




