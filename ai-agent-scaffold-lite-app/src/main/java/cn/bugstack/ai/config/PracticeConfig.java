package cn.bugstack.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PracticeConfig {

//    // ===== iFLYTEK ASR =====
//    @Value("${practice.iflytek.app-id:}")
//    private String xfyunAppId;
//    @Value("${practice.iflytek.api-key:}")
//    private String xfyunApiKey;
//    @Value("${practice.iflytek.api-secret:}")
//    private String xfyunApiSecret;
//
//    @Bean(initMethod = "init", destroyMethod = "shutdown")
//    public IAliyunAsrService xfyunAsrService() {
//        return new XfyunAsrService(xfyunAppId, xfyunApiKey, xfyunApiSecret);
//    }
//
//    // ===== TTS =====
//    @Value("${practice.tts.python-path:}")
//    private String ttsPythonPath;
//    @Value("${practice.tts.ffplay-path:}")
//    private String ttsFfplayPath;
//    @Value("${practice.tts.voice:en-US-JennyNeural}")
//    private String ttsVoice;
//
//    @Bean(initMethod = "start", destroyMethod = "stop")
//    public ITtsService ttsService() {
//        return new TtsService(ttsPythonPath, ttsFfplayPath, ttsVoice);
//    }
//
    // ===== DeepSeek Chat (Evaluation) =====
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String chatBaseUrl;
    @Value("${spring.ai.openai.api-key:}")
    private String chatApiKey;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .completionsPath("v1/chat/completions")
                .build();
    }

    @Primary
    @Bean("practiceChatModel")
    public ChatModel chatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.7d)
                        .build())
                .build();
    }

    @Bean("chatChatMemory")
    public ChatMemory chatChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean("evalChatMemory")
    public ChatMemory evalChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    @Bean("evlChatModel")
    public ChatModel evalchatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.7d)
                        .build())
                .build();
    }
}




