package cn.bugstack.ai.domain.practice.service;

public interface IChatLlmService {
    String chat(String userText);
    String chat(String userText, String systemPrompt);

    /** 注册会话的系统提示词 */
    void registerSession(String sessionId, String systemPrompt);

    /** 获取会话的系统提示词 */
    String getSessionPrompt(String sessionId);
}
