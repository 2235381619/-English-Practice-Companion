package cn.bugstack.ai.domain.practice.service;

public interface IChatLlmService {
    String chat(String userText);
    String chat(String userText, String systemPrompt);

    /** 注册场景，创建预配置的 ChatClient */
    void chatRegister(String sessionId, String scenarioCode);

    /** 通过已注册的 ChatClient 进行对话 */
    String chatBySession(String userText, String sessionId);
}
