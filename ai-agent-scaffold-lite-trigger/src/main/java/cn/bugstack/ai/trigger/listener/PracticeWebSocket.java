package cn.bugstack.ai.trigger.listener;

import cn.bugstack.ai.usecase.practice.IPracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 口语练习 WebSocket — 音频流对接
 *
 * ws://host:port/practice/audio/{sessionId}
 * 客户端发送二进制音频帧（16kHz 16bit PCM），
 * 服务端返回评测结果的 JSON 文本。
 */
@Slf4j
@Component
@ServerEndpoint("/practice/audio/{sessionId}")
public class PracticeWebSocket {

    /** 每个 session 的音频累积缓冲区 */
    private static final ConcurrentHashMap<String, ByteArrayOutputStream> audioBuffers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Session> liveSessions = new ConcurrentHashMap<>();

    private static IPracticeService practiceService;

    /**
     * 由于 WebSocket 不是 Spring 管理的（每次连接新实例），
     * 通过静态方法注入 Spring Bean。
     */
    @Resource
    public void setPracticeService(IPracticeService service) {
        PracticeWebSocket.practiceService = service;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        log.info("Practice WS open: sessionId={}", sessionId);
        liveSessions.put(sessionId, session);
        audioBuffers.put(sessionId, new ByteArrayOutputStream());
    }

    @OnMessage
    public void onMessage(byte[] audioChunk, Session session, @PathParam("sessionId") String sessionId) {
        try {
            ByteArrayOutputStream buf = audioBuffers.get(sessionId);
            if (buf == null) return;

            // 累积音频数据，等前端发送结束标记
            buf.write(audioChunk);
        } catch (Exception e) {
            log.error("Practice WS message error: sessionId={}", sessionId, e);
        }
    }

    @OnMessage
    public void onMessage(String text, Session session, @PathParam("sessionId") String sessionId) {
        try {
            // 文本消息作为标记： "END" 表示音频结束，触发 ASR + 评测
            if ("END".equalsIgnoreCase(text.trim())) {
                ByteArrayOutputStream buf = audioBuffers.get(sessionId);
                if (buf == null || buf.size() == 0) {
                    sendJson(session, "{\"error\":\"no audio data\"}");
                    return;
                }

                byte[] audioData = buf.toByteArray();
                buf.reset();

                var eval = practiceService.processAudio(sessionId, audioData);
                String json = String.format(
                        "{\"asrText\":\"%s\",\"correctedText\":\"%s\",\"score\":%d,\"aiReply\":\"%s\"}",
                        escapeJson(eval.getOriginalText()),
                        escapeJson(eval.getCorrectedText()),
                        eval.getScore(),
                        escapeJson(eval.getAiReply())
                );
                sendJson(session, json);
            }
        } catch (Exception e) {
            log.error("Practice WS process error: sessionId={}", sessionId, e);
            sendJson(session, "{\"error\":\"processing failed\"}");
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId) {
        log.info("Practice WS close: sessionId={}", sessionId);
        liveSessions.remove(sessionId);
        audioBuffers.remove(sessionId);
    }

    @OnError
    public void onError(Session session, @PathParam("sessionId") String sessionId, Throwable error) {
        log.error("Practice WS error: sessionId={}", sessionId, error);
        liveSessions.remove(sessionId);
        audioBuffers.remove(sessionId);
    }

    private void sendJson(Session session, String json) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            log.warn("Send WS message failed", e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
