package cn.bugstack.ai.config;

import cn.bugstack.ai.domain.practice.event.EvaluationResultPublisher;
import cn.bugstack.ai.trigger.listener.PracticeAudioWebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞语音客户端 & 评测回调 配置
 */
@Slf4j
@Configuration
public class IflytekConfiguration {

    @PostConstruct
    public void registerEvalCallback() {
        EvaluationResultPublisher.setCallback(PracticeAudioWebSocketHandler::sendEvalResult);
        log.info("EvaluationResultPublisher callback registered -> PracticeAudioWebSocketHandler");
    }
}
