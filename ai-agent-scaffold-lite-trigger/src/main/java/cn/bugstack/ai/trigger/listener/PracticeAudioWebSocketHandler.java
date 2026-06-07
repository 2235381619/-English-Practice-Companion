package cn.bugstack.ai.trigger.listener;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.event.EvaluationResultPublisher;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 口语练习 WebSocket Handler — 基于 Spring WebSocket API
 *
 * ws://host:port/practice/audio/{sessionId}
 * 客户端发送二进制 PCM 帧（16kHz 16bit Mono），
 * 发送文本 "END" 触发 ASR + 评测，返回评测 JSON。
 */
@Slf4j
@Component
public class PracticeAudioWebSocketHandler extends AbstractWebSocketHandler {

    private static final ConcurrentHashMap<String, ByteArrayOutputStream> audioBuffers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> sessionScenarios = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, java.util.List<java.util.Map<String, Object>>> sessionRounds = new ConcurrentHashMap<>();

    private final DefaultPracticeFactory practiceFactory;

    public PracticeAudioWebSocketHandler(DefaultPracticeFactory practiceFactory) {
        this.practiceFactory = practiceFactory;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        log.info("Practice WS open: sessionId={}", sessionId);
        liveSessions.put(sessionId, session);
        audioBuffers.put(sessionId, new ByteArrayOutputStream());
        session.setBinaryMessageSizeLimit(1024 * 1024);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = extractSessionId(session);
        try {
            ByteArrayOutputStream buf = audioBuffers.get(sessionId);
            if (buf == null) return;
            buf.write(message.getPayload().array());
        } catch (Exception e) {
            log.error("Practice WS binary error: sessionId={}", sessionId, e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        String sessionId = extractSessionId(session);
        try {
            String text = textMessage.getPayload().trim();
            log.info("Practice WS text: sessionId={}, text={}", sessionId, text);

            if (text.startsWith("{")) {
                // JSON config message: { "type": "config", "scenarioCode": "interview" }
                try {
                    String scenarioCode = com.alibaba.fastjson.JSON.parseObject(text).getString("scenarioCode");
                    if (scenarioCode == null || scenarioCode.isBlank()) scenarioCode = "default";
                    sessionScenarios.put(sessionId, scenarioCode);
                    log.info("Practice WS config: sessionId={}, scenario={}", sessionId, scenarioCode);
                } catch (Exception e) {
                    log.warn("Failed to parse config JSON: {}", e.getMessage());
                }
            } else if ("END".equalsIgnoreCase(text)) {
                processAudioAndReply(session, sessionId);
            }
        } catch (Exception e) {
            log.error("Practice WS text error: sessionId={}", sessionId, e);
            sendJson(session, "{\"error\":\"processing failed\"}");
        }
    }

    private void processAudioAndReply(WebSocketSession session, String sessionId) {
        ByteArrayOutputStream buf = audioBuffers.get(sessionId);
        if (buf == null || buf.size() == 0) {
            sendJson(session, "{\"error\":\"no audio data\"}");
            return;
        }

        byte[] audioData = buf.toByteArray();
        log.info("Audio buffer size: {} bytes", audioData.length);
        audioBuffers.put(sessionId, new ByteArrayOutputStream());

        try {
            String scenarioCode = sessionScenarios.getOrDefault(sessionId, "default");
            HandlePracticeMessageCommandEntity req = HandlePracticeMessageCommandEntity.builder()
                    .sessionId(sessionId)
                    .inputType(1)
                    .audioData(audioData)

                    .build();

            DefaultPracticeFactory.DynamicContext ctx = new DefaultPracticeFactory.DynamicContext();
            ctx.setSessionId(sessionId);

            StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> handler =
                    practiceFactory.strategyHandler();
            handler.apply(req, ctx);

            com.alibaba.fastjson.JSONObject resp = new com.alibaba.fastjson.JSONObject();
            resp.put("asrText", ctx.getAsrText() != null ? ctx.getAsrText() : "");
            resp.put("replyText", ctx.getReplyText() != null ? ctx.getReplyText() : "");
            resp.put("correctedText", ctx.getCorrectedText() != null ? ctx.getCorrectedText() : "");
            resp.put("grammarIssues", ctx.getGrammarIssues() != null ? ctx.getGrammarIssues() : new java.util.ArrayList());
            resp.put("suggestions", ctx.getSuggestions() != null ? ctx.getSuggestions() : new java.util.ArrayList());
            resp.put("score", ctx.getScore());
            resp.put("audioUrl", ctx.getAudioUrl() != null ? ctx.getAudioUrl() : "");
            resp.put("audioData", ctx.getAudioData() != null ? ctx.getAudioData() : "");
            sendJson(session, resp.toJSONString());
            log.info("Audio processed: sessionId={}, asrText=\"{}\", replyText=\"{}\"",
                    sessionId, ctx.getAsrText(), ctx.getReplyText());
        } catch (Exception e) {
            log.error("Process audio failed: sessionId={}", sessionId, e);
            sendJson(session, "{\"error\":\"processing error\"}");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        log.info("Practice WS close: sessionId={}, status={}", sessionId, status);
        liveSessions.remove(sessionId);
        audioBuffers.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = extractSessionId(session);
        log.error("Practice WS error: sessionId={}", sessionId, exception);
        liveSessions.remove(sessionId);
        audioBuffers.remove(sessionId);
    }

    // ── helpers ──

    public static void saveRound(String sessionId, java.util.Map<String, Object> round) {
        sessionRounds.computeIfAbsent(sessionId, k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>())).add(round);
    }

    public static java.util.List<java.util.Map<String, Object>> getSessionRounds(String sessionId) {
        return sessionRounds.get(sessionId);
    }

    private String extractSessionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return "unknown";
        String path = uri.getPath();
        // /practice/audio/{sessionId}
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : "unknown";
    }

    private void sendJson(WebSocketSession session, String json) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
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

    /**
     * 异步评测结果推送到前端 WebSocket
     */
    public static void sendEvalResult(String sessionId, EvaluationResult eval) {
        WebSocketSession session = liveSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
                json.put("type", "evaluation");
                json.put("correctedText", eval.getCorrectedText() != null ? eval.getCorrectedText() : "");
                json.put("grammarIssues", eval.getGrammarIssues() != null ? eval.getGrammarIssues() : new java.util.ArrayList());
                json.put("suggestions", eval.getSuggestions() != null ? eval.getSuggestions() : new java.util.ArrayList());
                json.put("score", eval.getScore());
                session.sendMessage(new TextMessage(json.toJSONString()));
            } catch (Exception e) {
                log.warn("Send eval result failed: sessionId={}", sessionId, e);
            }
        }
    }
}
