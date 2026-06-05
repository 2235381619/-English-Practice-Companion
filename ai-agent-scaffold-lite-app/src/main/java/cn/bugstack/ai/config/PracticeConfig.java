package cn.bugstack.ai.config;

import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 口语陪练 Bean 装配配置
 *
 * 创建 ASR (Whisper) 所需的 OpenAiAudioApi bean，
 * 供 domain 层的 AudioService 通过构造器注入。
 */
@Configuration
public class PracticeConfig {

    @Value("${practice.asr.base-url:https://api.openai.com}")
    private String asrBaseUrl;

    @Value("${practice.asr.api-key:}")
    private String asrApiKey;

    @Bean
    public OpenAiAudioApi openAiAudioApi() {
        return OpenAiAudioApi.builder()
                .baseUrl(asrBaseUrl)
                .apiKey(asrApiKey)
                .build();
    }
}
