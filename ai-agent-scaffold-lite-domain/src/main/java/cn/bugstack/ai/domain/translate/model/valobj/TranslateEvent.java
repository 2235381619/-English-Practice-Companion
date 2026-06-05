package cn.bugstack.ai.domain.translate.model.valobj;

import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import lombok.Getter;

@Getter
public class TranslateEvent {

    public enum EventType {
        TRANSLATION,
        CORRECTION,
        STATUS
    }

    private final EventType type;
    private final TranslationSegmentEntity segment;
    private final boolean partial;

    // For STATUS events
    private final String status;

    // For CORRECTION events
    private final String segmentId;
    private final int sequenceNum;
    private final String correctedSource;
    private final String correctedTarget;
    private final String correctionReason;

    private TranslateEvent(EventType type, TranslationSegmentEntity segment, boolean partial,
                           String status, String segmentId, int sequenceNum,
                           String correctedSource, String correctedTarget,
                           String correctionReason) {
        this.type = type;
        this.segment = segment;
        this.partial = partial;
        this.status = status;
        this.segmentId = segmentId;
        this.sequenceNum = sequenceNum;
        this.correctedSource = correctedSource;
        this.correctedTarget = correctedTarget;
        this.correctionReason = correctionReason;
    }

    public static TranslateEvent translation(TranslationSegmentEntity segment, boolean partial) {
        return new TranslateEvent(EventType.TRANSLATION, segment, partial,
                null, null, 0, null, null, null);
    }

    public static TranslateEvent correction(String segmentId, int sequenceNum,
                                             String correctedSource, String correctedTarget,
                                             String reason) {
        return new TranslateEvent(EventType.CORRECTION, null, false,
                null, segmentId, sequenceNum, correctedSource, correctedTarget, reason);
    }

    public static TranslateEvent status(String status, String message) {
        return new TranslateEvent(EventType.STATUS, null, false,
                status, null, 0, null, null, message);
    }

    // Convenience accessors
    public String getSourceText() {
        return segment != null ? segment.getSourceText() : null;
    }

    public String getTargetText() {
        return segment != null ? segment.getTargetText() : null;
    }

    public boolean isPartial() {
        return partial;
    }
}
