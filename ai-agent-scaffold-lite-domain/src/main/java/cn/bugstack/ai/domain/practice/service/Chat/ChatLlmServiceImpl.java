package cn.bugstack.ai.domain.practice.service.Chat;

import cn.bugstack.ai.domain.practice.service.IChatLlmService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatLlmServiceImpl implements IChatLlmService {

    private static final String CHAT_TEMPLATE = "{systemPrompt}\n\nUser: {userText}\nAssistant:";

    private static final java.util.Map<String, String> SCENARIO_PROMPTS = java.util.Map.of(
        "default", "You are a friendly English conversation partner. Keep responses natural and concise (1-3 sentences). Just have a natural conversation.",
        "interview", "You are a senior tech interviewer. Ask interview questions, evaluate answers, and provide feedback. Keep responses concise (1-3 sentences).",
        "restaurant", "You are a waiter at a restaurant. Take orders, answer questions about the menu, and make small talk. Keep responses concise (1-3 sentences).",
        "meeting", "You are a business professional in a meeting. Discuss topics professionally, ask questions, and provide feedback. Keep responses concise (1-3 sentences)."
    );

    @Resource(name = "practiceChatModel")
    private ChatModel chatModel;


    @Override
    public String chat(String userText) {
        return chat(userText, "You are a friendly English conversation partner. Respond naturally and concisely.");
    }


    @Override
    public String getSessionPrompt(String sessionId) {
        return sessionPrompts.get(sessionId);
    }

    @Override
    public void chatRegister(String sessionId, String scenarioCode) {
        String scenario = scenarioCode != null ? scenarioCode : "default";
        String prompt = SCENARIO_PROMPTS.getOrDefault(scenario, SCENARIO_PROMPTS.get("default"));
        sessionPrompts.put(sessionId, prompt);
        log.info("Scenario registered: sessionId={}, scenario={}", sessionId, scenario);
    }

    @Override
    public String chat(String userText, String systemPrompt) {
        if (userText == null || userText.isBlank()) {
            return "I didn't catch that. Could you repeat?";
        }
        try {
            PromptTemplate template = new PromptTemplate(CHAT_TEMPLATE);
            Prompt prompt = template.create(Map.of(
                "systemPrompt", systemPrompt != null ? systemPrompt : "",
                "userText", userText
            ));
            String reply = chatModel.call(prompt).getResult().getOutput().getText();
            log.info("ChatLlm: len={} -> len={}", userText.length(), reply != null ? reply.length() : 0);
            return reply != null ? reply.trim() : "";
        } catch (Exception e) {
            log.error("ChatLlm failed: {}", e.getMessage());
            return "Sorry, I'm having trouble responding.";
        }
    }
}
