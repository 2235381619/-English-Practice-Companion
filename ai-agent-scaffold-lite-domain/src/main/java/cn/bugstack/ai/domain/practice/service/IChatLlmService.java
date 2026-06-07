package cn.bugstack.ai.domain.practice.service;

public interface IChatLlmService {
    String chat(String userText);
    String chat(String userText, String systemPrompt);

    /** 获取会话的系统提示词 */
    String getSessionPrompt(String sessionId);

    /** 注册场景，设定会话的提示词 */
    void chatRegister(String sessionId, String scenarioCode);
}
