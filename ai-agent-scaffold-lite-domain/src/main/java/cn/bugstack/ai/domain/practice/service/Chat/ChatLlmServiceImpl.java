package cn.bugstack.ai.domain.practice.service.Chat;

import cn.bugstack.ai.domain.practice.service.IChatLlmService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ChatLlmServiceImpl implements IChatLlmService {

    private static final String CHAT_TEMPLATE = "{systemPrompt}\n\nUser: {userText}\nAssistant:";

    @Resource(name = "practiceChatModel")
    private ChatModel chatModel;


    @Override
    public String chat(String userText) {
        return chat(userText, "You are a friendly English conversation partner. Respond naturally and concisely.");
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
