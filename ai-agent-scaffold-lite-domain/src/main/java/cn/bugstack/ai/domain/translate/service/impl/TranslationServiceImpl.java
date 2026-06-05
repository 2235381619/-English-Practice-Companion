package cn.bugstack.ai.domain.translate.service.impl;

import cn.bugstack.ai.domain.translate.agent.TranscriberAgent;
import cn.bugstack.ai.domain.translate.agent.TranslatorAgent;
import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import cn.bugstack.ai.domain.translate.model.valobj.TranslateEvent;
import reactor.core.publisher.Flux;
import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TranslationServiceImpl {

    private final ConcurrentHashMap<String, TranslationSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionIdGen = new AtomicInteger(0);
    private final TranscriberAgent transcriberAgent;
    private final TranslatorAgent translatorAgent;

    public TranslationServiceImpl(TranscriberAgent transcriberAgent, TranslatorAgent translatorAgent) {
        this.transcriberAgent = transcriberAgent;
        this.translatorAgent = translatorAgent;
    }

    public String createSession(String sourceLang, String targetLang) {
        int id = sessionIdGen.incrementAndGet();
        String sessionId = String.format("%012d", id);
        TranslationSession session = new TranslationSession(sessionId, sourceLang, targetLang);
        sessions.put(sessionId, session);
        session.emit(TranslateEvent.status("ready", "Session created"));
        return sessionId;
    }

    public void closeSession(String sessionId) {
        TranslationSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    public Flux<TranslateEvent> processAudioChunk(String sessionId, int sequenceNum,
                                                    byte[] audioData, String mimeType) {
        TranslationSession session = sessions.get(sessionId);
        if (session == null) {
            return Flux.just(TranslateEvent.status("error", "Session not found: " + sessionId));
        }

        // Transcribe
        String transcribed = transcriberAgent.transcribe(sessionId, audioData, mimeType);

        if (transcribed == null || transcribed.isBlank()) {
            return Flux.just(TranslateEvent.status("empty", "No speech detected"));
        }

        // Create segment entity
        String segId = session.nextSegmentId();
        TranslationSegmentEntity segment = new TranslationSegmentEntity(
                segId, sequenceNum, session.getSourceLang(), session.getTargetLang(), transcribed);

        // Translate
        return translatorAgent.translate(transcribed, session.getSourceLang(),
                        session.getTargetLang(), sessionId)
                .collect(StringBuilder::new, StringBuilder::append)
                .flatMapMany(fullTranslation -> {
                    segment.setTargetText(fullTranslation.toString());
                    segment.setStatus(SegmentStatus.COMPLETED);
                    session.getBuffer().append(segment);

                    TranslateEvent event = TranslateEvent.translation(segment, false);
                    session.emit(event);

                    return Flux.just(event);
                });
    }

    public Flux<TranslateEvent> getEventStream(String sessionId) {
        TranslationSession session = sessions.get(sessionId);
        if (session == null) {
            return Flux.empty();
        }
        return session.getEventStream();
    }

    public String exportSession(String sessionId) {
        TranslationSession session = sessions.get(sessionId);
        if (session == null) return "";
        return session.exportAsText();
    }
}
