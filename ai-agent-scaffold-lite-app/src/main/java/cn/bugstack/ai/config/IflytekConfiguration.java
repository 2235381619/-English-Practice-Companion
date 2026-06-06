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
    public TtsClient ttsClient(IflytekProperties props) {
        TtsClient.Builder builder = new TtsClient.Builder()
                .signature(props.getAppId(), props.getApiKey(), props.getApiSecret())
                .aue(props.getAue())
                .auf(props.getAuf())
                .vcn(props.getVoiceName())
                .speed(Integer.parseInt(props.getSpeed()))
                .volume(Integer.parseInt(props.getVolume()))
                .pitch(Integer.parseInt(props.getPitch()))
                .tte(props.getTte());

        // aue=lame 时必须设置 sfl=1 开启流式 mp3
        if ("lame".equals(props.getAue())) {
            builder.sfl(1);
        }

        TtsClient client = builder.build();
        log.info("讯飞 TtsClient 创建成功, voice={}, aue={}, auf={}", props.getVoiceName(), props.getAue(), props.getAuf());
        return client;
    }

//    @Bean
//    public IatClient iatClient(IflytekProperties props) {
//        IatClient client = new IatClient.Builder()
//                .signature(props.getAppId(), props.getApiKey(), props.getApiSecret())
//                .dwa("wpgs")          // 动态修正：流式识别中持续优化已返回的文字
//                .build();
//        log.info("讯飞 IatClient 创建成功, appId={}", props.getAppId());
//        return client;
//    }
}
