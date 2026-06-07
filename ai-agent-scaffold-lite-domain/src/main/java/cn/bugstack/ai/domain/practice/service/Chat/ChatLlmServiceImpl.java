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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

@Slf4j
@Service
public class ChatLlmServiceImpl implements IChatLlmService {
    private final ConcurrentHashMap<String, ChatClient> sessionClients = new ConcurrentHashMap<>();

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
    public String chatBySession(String userText, String sessionId) {
        ChatClient client = sessionClients.get(sessionId);
        if (client == null) {
            return chat(userText, "You are a friendly English conversation partner. Keep responses natural and concise (1-3 sentences).");
        }
        try {
            String reply = client.prompt()
                    .user(userText)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
            log.info("ChatLlm: len={} -> len={}", userText.length(), reply != null ? reply.length() : 0);
            return reply != null ? reply.trim() : "";
        } catch (Exception e) {
            log.error("ChatLlm failed: {}", e.getMessage());
            return "Sorry, I'm having trouble responding.";
        }
    }

    @Override
    public void chatRegister(String sessionId, String scenarioCode) {
        String scenario = scenarioCode != null ? scenarioCode : "default";
        String prompt = SCENARIO_PROMPTS.getOrDefault(scenario, SCENARIO_PROMPTS.get("default"));
        ChatClient client = ChatClient.builder(chatModel)
                .defaultSystem(prompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        chatMemory.clear(sessionId);
        sessionClients.put(sessionId, client);
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


