package cn.bugstack.ai.config;

import cn.bugstack.ai.domain.practice.adapter.ISessionRepository;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import cn.bugstack.ai.domain.practice.service.IAliyunAsrService;
import cn.bugstack.ai.domain.practice.service.impl.AliyunAsrService;
import cn.bugstack.ai.domain.practice.service.ITtsService;
import cn.bugstack.ai.domain.practice.service.impl.TtsService;
import cn.bugstack.ai.usecase.practice.IPracticeService;
import cn.bugstack.ai.usecase.practice.PracticeServiceImpl;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 口语陪练 Bean 装配配置
 */
@Configuration
public class PracticeConfig {

//    // ===== ASR (Whisper) =====
//    @Value("${practice.asr.base-url:https://api.openai.com}")
//    private String asrBaseUrl;
//    @Value("${practice.asr.api-key:}")
//    private String asrApiKey;
//
//    @Bean
//    public OpenAiAudioApi openAiAudioApi() {
//        return OpenAiAudioApi.builder()
//                .baseUrl(asrBaseUrl)
//                .apiKey(asrApiKey)
//                .build();
//    }

    // ===== Chat (GPT 评测) =====
    @Value("${spring.ai.openai.base-url}")
    private String chatBaseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String chatApiKey;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .completionsPath("v1/chat/completions")
                .build();
    }

    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.7d)
                        .build())
                .build();
    }


    // ===== NLS ASR =====
    @Value("${practice.asr.nls.app-key:}")
    private String nlsAppKey;
    @Value("${practice.asr.nls.access-key-id:}")
    private String nlsAccessKeyId;
    @Value("${practice.asr.nls.access-key-secret:}")
    private String nlsAccessKeySecret;
    @Value("${practice.asr.nls.gateway-url:}")
    private String nlsGatewayUrl;

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public IAliyunAsrService aliyunAsrService() {
        return new AliyunAsrService(nlsAppKey, nlsAccessKeyId, nlsAccessKeySecret, nlsGatewayUrl);
    }
    // ===== TTS =====
    @Value("${practice.tts.python-path:}")
    private String ttsPythonPath;
    @Value("${practice.tts.ffplay-path:}")
    private String ttsFfplayPath;
    @Value("${practice.tts.voice:en-US-JennyNeural}")
    private String ttsVoice;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ITtsService ttsService() {
        return new TtsService(ttsPythonPath, ttsFfplayPath, ttsVoice);
    }

    // ===== 用例编排 =====
    @Bean
    public IPracticeService practiceService(//IAudioService audioService,
                                              IAliyunAsrService aliyunAsrService,
                                             IEvaluationService evaluationService,
                                             ITtsService ttsService,
                                             ISessionRepository sessionRepository) {
        return new PracticeServiceImpl(//audioService,
                 aliyunAsrService, evaluationService, ttsService, sessionRepository);
    }
}

