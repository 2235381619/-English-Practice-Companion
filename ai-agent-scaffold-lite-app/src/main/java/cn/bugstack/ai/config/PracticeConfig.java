package cn.bugstack.ai.config;

import cn.bugstack.ai.domain.practice.adapter.ISessionRepository;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import cn.bugstack.ai.domain.practice.service.IAliyunAsrService;
import cn.bugstack.ai.domain.practice.service.impl.AliyunAsrService;
import cn.bugstack.ai.domain.practice.service.impl.XfyunAsrService;
import cn.bugstack.ai.domain.iflytek.service.impl.IatServiceImpl;
import cn.bugstack.ai.domain.practice.service.ITtsService;
import cn.bugstack.ai.domain.practice.service.impl.TtsService;
import cn.bugstack.ai.domain.iflytek.service.impl.TtsServiceImpl;
import cn.xfyun.api.IatClient;
import cn.xfyun.api.TtsClient;
import cn.bugstack.ai.usecase.practice.IPracticeService;
import cn.bugstack.ai.usecase.practice.PracticeServiceImpl;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * 口语陪练 Bean 装配配置
 */
@Slf4j
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


    // ===== 语音识别 ASR =====

    // 讯飞 IAT ASR
    @Bean(initMethod = "init", destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "voice.asr.provider", havingValue = "iflytek")
    public IAliyunAsrService iflytekAsrService(IatClient iatClient) {
        log.info("使用讯飞 IAT 实时语音识别");
        return new IatServiceImpl(iatClient);
    }

    // 阿里云 NLS ASR（默认）
    @Value("${practice.asr.nls.app-key:}")
    private String nlsAppKey;
    @Value("${practice.asr.nls.access-key-id:}")
    private String nlsAccessKeyId;
    @Value("${practice.asr.nls.access-key-secret:}")
    private String nlsAccessKeySecret;
    @Value("${practice.asr.nls.gateway-url:}")
    private String nlsGatewayUrl;

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "voice.asr.provider", havingValue = "aliyun", matchIfMissing = true)
    public IAliyunAsrService aliyunAsrService() {
        return new AliyunAsrService(nlsAppKey, nlsAccessKeyId, nlsAccessKeySecret, nlsGatewayUrl);
    }

    // ===== 语音合成 TTS =====

    // 讯飞 TTS
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "voice.tts.provider", havingValue = "iflytek")
    public ITtsService iflytekTtsService(TtsClient ttsClient) {
        return new TtsServiceImpl(ttsClient);
    }

    // Edge-TTS（默认）
    @Value("${practice.tts.python-path:}")
    private String ttsPythonPath;
    @Value("${practice.tts.ffplay-path:}")
    private String ttsFfplayPath;
    @Value("${practice.tts.voice:en-US-JennyNeural}")
    private String ttsVoice;

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "voice.tts.provider", havingValue = "edgetts", matchIfMissing = true)
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


