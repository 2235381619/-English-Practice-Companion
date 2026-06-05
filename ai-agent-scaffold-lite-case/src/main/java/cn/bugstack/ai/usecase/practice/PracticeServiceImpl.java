package cn.bugstack.ai.usecase.practice;

import cn.bugstack.ai.domain.practice.adapter.ISessionRepository;
import cn.bugstack.ai.domain.practice.model.entity.ConversationRound;
import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.model.valobj.SessionReport;
import cn.bugstack.ai.domain.practice.service.IAliyunAsrService;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import cn.bugstack.ai.domain.practice.service.ITtsService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 口语练习用例编排 — 用例层实现
 *
 * 串联 IAudioService → IEvaluationService → ITtsService，
 * 管理 PracticeSession 生命周期。
 */
@Slf4j
public class PracticeServiceImpl implements IPracticeService {

    //private final IAudioService audioService;
    private final IAliyunAsrService aliyunAsrService;
    private final IEvaluationService evaluationService;
    private final ITtsService ttsService;
    private final ISessionRepository sessionRepository;

    private final ConcurrentHashMap<String, PracticeSession> sessions = new ConcurrentHashMap<>();

    public PracticeServiceImpl(
                                //IAudioService audioService,
                                IAliyunAsrService aliyunAsrService,
                                IEvaluationService evaluationService,
                                ITtsService ttsService,
                                ISessionRepository sessionRepository) {
        //this.audioService = audioService;
        this.aliyunAsrService = aliyunAsrService;
        this.evaluationService = evaluationService;
        this.ttsService = ttsService;
        this.sessionRepository = sessionRepository;
    }

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
    public EvaluationResult processAudio(String sessionId, byte[] audioData) {
        PracticeSession session = getSession(sessionId);
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("Session not found or inactive: " + sessionId);
        }

        String text;
        try {
            text = aliyunAsrService.transcribe(audioData, "webm");
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
