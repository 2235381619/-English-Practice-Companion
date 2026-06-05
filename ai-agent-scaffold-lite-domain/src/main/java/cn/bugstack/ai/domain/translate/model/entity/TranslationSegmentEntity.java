package cn.bugstack.ai.domain.translate.model.entity;

import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TranslationSegmentEntity {

    private final String segmentId;
    private final int sequenceNum;
    private final String sourceLang;
    private final String targetLang;

    @Setter
    private String sourceText;

    @Setter
    private String targetText;

    @Setter
    private SegmentStatus status;

    @Setter
    private int version = 1;

    public TranslationSegmentEntity(String segmentId, int sequenceNum,
                                     String sourceLang, String targetLang,
                                     String sourceText) {
        this.segmentId = segmentId;
        this.sequenceNum = sequenceNum;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.sourceText = sourceText;
        this.status = SegmentStatus.PENDING;
    }

    public TranslationSegmentEntity(String segmentId, int sequenceNum,
                                     String sourceLang, String targetLang,
                                     String sourceText, String targetText) {
        this(segmentId, sequenceNum, sourceLang, targetLang, sourceText);
        this.targetText = targetText;
        this.status = SegmentStatus.COMPLETED;
    }
}
