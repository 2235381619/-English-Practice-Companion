package cn.bugstack.ai.usecase.practice.prectice;

import cn.bugstack.ai.domain.practice.adapter.ISessionRepository;
import cn.bugstack.ai.domain.practice.model.entity.ConversationRound;
import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.model.valobj.SessionReport;
import cn.bugstack.ai.domain.practice.service.IAsrService;
import cn.bugstack.ai.usecase.practice.IPracticeService;
import jakarta.annotation.Resource;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import cn.bugstack.ai.domain.practice.service.ITtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 口语练习用例编排 — 用例层实现
 *
 * 串联 IAudioService → IEvaluationService → ITtsService，
 * 管理 PracticeSession 生命周期。
 */
@Slf4j
@Service
public class PracticeServiceImpl implements IPracticeService {

    @Resource
    private  IEvaluationService evaluationService;
    @Resource
    private ITtsService ttsService;
    @Resource
    private IAsrService asrService;
    @Resource
    private  ISessionRepository sessionRepository;

    private final ConcurrentHashMap<String, PracticeSession> sessions = new ConcurrentHashMap<>();
    @Override
    public PracticeSession createSession(Scenario scenario) {
        PracticeSession session = new PracticeSession(scenario);
        sessions.put(session.getSessionId(), session);
        if (sessionRepository != null) {
            sessionRepository.save(session);
        }
        log.info("Practice session created: id={}, scenario={}", session.getSessionId(), scenario.getCode());
        return session;
    }

    @Override
    public PracticeSession getSession(String sessionId) {
        PracticeSession session = sessions.get(sessionId);
        if (session == null && sessionRepository != null) {
            session = sessionRepository.findById(sessionId);
        }
        return session;
    }

    @Override
    public EvaluationResult processAudio(String sessionId, File audioFile) {
        PracticeSession session = getSession(sessionId);
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("Session not found or inactive: " + sessionId);
        }
        String text;
        try {
            text = asrService.transcribe(audioFile);
        }
        catch (Exception e) {
            log.warn("ASR failed: {}", e.getMessage()); text = null;
        }
        if (text == null || text.isBlank()) {
            return EvaluationResult.builder().originalText("").aiReply("Sorry, I didn't catch that.").score(0).build();
        }
        return processTextInternal(session, text);
    }

    @Override
    public EvaluationResult processAudio(String sessionId, byte[] audioData) {
        PracticeSession session = getSession(sessionId);
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("Session not found or inactive: " + sessionId);
        }

        String text;
        try {
            File temp = File.createTempFile("audio_", ".pcm");
            try {
                java.nio.file.Files.write(temp.toPath(), audioData);
                text = asrService.transcribe(temp);
            } finally {
                temp.delete();
            }
        } catch (Exception e) {
            log.warn("ASR failed: {}", e.getMessage());
            text = null;
        }
        if (text == null || text.isBlank()) {
            log.info("No speech detected in audio for session {}", sessionId);
            return EvaluationResult.builder()
                    .originalText("")
                    .aiReply("Sorry, I didn't catch that. Could you please repeat?")
                    .score(0)
                    .build();
        }

        return processTextInternal(session, text);
    }

    @Override
    public EvaluationResult processText(String sessionId, String text) {
        PracticeSession session = getSession(sessionId);
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("Session not found or inactive: " + sessionId);
        }
        return processTextInternal(session, text);
    }

    private EvaluationResult processTextInternal(PracticeSession session, String text) {
        String history = buildHistory(session);
        ConversationRound round = session.addRound(text);

        EvaluationResult eval = evaluationService.evaluate(text, session.getScenario(), history);
        round.setEvaluation(eval);
        round.setAiReply(eval.getAiReply());
        round.setAsrText(text);

        if (ttsService != null && ttsService.isRunning() && eval.getAiReply() != null) {
            ttsService.speak(eval.getAiReply());
        }

        if (sessionRepository != null) {
            sessionRepository.update(session);
        }

        log.info("Practice round {}: text='{}' score={}",
                round.getRoundNumber(), text, eval.getScore());
        return eval;
    }

    @Override
    public SessionReport getReport(String sessionId) {
        PracticeSession session = getSession(sessionId);
        if (session == null) return null;
        return session.generateReport();
    }

    @Override
    public void closeSession(String sessionId) {
        PracticeSession session = sessions.get(sessionId);
        if (session != null) {
            session.close();
            if (sessionRepository != null) {
                sessionRepository.update(session);
            }
            log.info("Practice session closed: {}", sessionId);
        }
    }

    private String buildHistory(PracticeSession session) {
        StringBuilder sb = new StringBuilder();
        for (ConversationRound r : session.getRounds()) {
            if (r.getAsrText() != null) {
                sb.append("User: ").append(r.getAsrText()).append("\n");
            }
            if (r.getAiReply() != null) {
                sb.append("Tutor: ").append(r.getAiReply()).append("\n");
            }
        }
        return sb.toString();
    }
}
