package cn.bugstack.ai.domain.practice.service;

public interface IChatLlmService {
    String chat(String userText);
    String chat(String userText, String systemPrompt);
}
