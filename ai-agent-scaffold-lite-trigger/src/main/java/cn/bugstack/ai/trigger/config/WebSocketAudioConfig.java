package cn.bugstack.ai.trigger.config;

import cn.bugstack.ai.trigger.listener.PracticeAudioWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketAudioConfig implements WebSocketConfigurer {

    private final PracticeAudioWebSocketHandler audioWebSocketHandler;

    public WebSocketAudioConfig(PracticeAudioWebSocketHandler audioWebSocketHandler) {
        this.audioWebSocketHandler = audioWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/practice/audio/{sessionId}")
                .setAllowedOrigins("*");
    }
}