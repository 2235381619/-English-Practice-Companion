package cn.bugstack.ai.config;

import cn.xfyun.api.IatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞语音客户端 Bean 装配
 *
 * TtsClient 已在 domain 层 TtsService 内部自行创建。
 */
@Slf4j
@Configuration
public class IflytekConfiguration {

//    @Bean
//    public IatClient iatClient(IflytekProperties props) {
//        IatClient client = new IatClient.Builder()
//                .signature(props.getAppId(), props.getApiKey(), props.getApiSecret())
//                .dwa("wpgs")
//                .build();
//        log.info("讯飞 IatClient 创建成功, appId={}", props.getAppId());
//        return client;
//    }
}
