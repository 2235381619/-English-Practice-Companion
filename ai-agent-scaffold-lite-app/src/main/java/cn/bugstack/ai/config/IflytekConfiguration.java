package cn.bugstack.ai.config;

import cn.xfyun.api.IatClient;
import cn.xfyun.api.TtsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞语音客户端 Bean 装配
 *
 * IatClient 和 TtsClient 是线程安全的，作为单例注册到容器中。
 * Domain 层直接注入使用。
 */
@Slf4j
@Configuration
public class IflytekConfiguration {

    @Bean
    public IatClient iatClient(IflytekProperties props) {
        IatClient client = new IatClient.Builder()
                .signature(props.getAppId(), props.getApiKey(), props.getApiSecret())
                .dwa("wpgs")          // 动态修正：流式识别中持续优化已返回的文字
                .build();
        log.info("讯飞 IatClient 创建成功, appId={}", props.getAppId());
        return client;
    }

    @Bean
    public TtsClient ttsClient(IflytekProperties props) {
        TtsClient client = new TtsClient.Builder()
                .signature(props.getAppId(), props.getApiKey(), props.getApiSecret())
                .vcn(props.getVoiceName())
                .speed(Integer.parseInt(props.getSpeed()))
                .volume(Integer.parseInt(props.getVolume()))
                .pitch(50)
                .build();
        log.info("讯飞 TtsClient 创建成功, voice={}", props.getVoiceName());
        return client;
    }
}
