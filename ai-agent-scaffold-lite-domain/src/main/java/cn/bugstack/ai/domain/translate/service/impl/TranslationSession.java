package cn.bugstack.ai.domain.translate.service.impl;

import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;
import cn.bugstack.ai.domain.translate.model.valobj.TranslateEvent;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TranslationSession {

    private final String sessionId;
    private final String sourceLang;
    private final String targetLang;
    private final SegmentBuffer buffer;
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final Sinks.Many<TranslateEvent> eventSink;
    private volatile boolean active = true;

    public TranslationSession(String sessionId, String sourceLang, String targetLang) {
        this.sessionId = sessionId;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.buffer = new SegmentBuffer(500);
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public boolean isActive() {
        return active;
    }

    public int currentSequence() {
        return sequence.get();
    }

    public String nextSegmentId() {
        int seq = sequence.incrementAndGet();
        return String.format("seg_%04d", seq);
    }

    public Flux<TranslateEvent> getEventStream() {
        return eventSink.asFlux();
    }

    public void emit(TranslateEvent event) {
        if (!active) return;
        eventSink.tryEmitNext(event);
    }

    public void close() {
        active = false;
        eventSink.tryEmitComplete();
    }

    public String exportAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Translation Session: ").append(sessionId).append(" ===\n");
        sb.append("Language: ").append(sourceLang).append(" \u2192 ").append(targetLang).append("\n\n");

        List<TranslationSegmentEntity> all = buffer.getAll();
        for (TranslationSegmentEntity seg : all) {
            sb.append("[").append(seg.getSequenceNum()).append("] ");
            if (seg.getStatus() == SegmentStatus.CORRECTED) {
                sb.append("[\u270F\uFE0F corrected] ");
            }
            sb.append(seg.getSourceText()).append("\n");
            sb.append("  \u2192 ").append(seg.getTargetText()).append("\n\n");
        }

        sb.append("--- ").append(all.size()).append(" segments total ---\n");
        return sb.toString();
    }
}
