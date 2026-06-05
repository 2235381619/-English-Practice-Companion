package cn.bugstack.ai.domain.translate.service.impl;

import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SegmentBuffer {

    private final int maxSize;
    private final List<TranslationSegmentEntity> segments;
    private int newSegmentCount = 0;

    public SegmentBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.segments = new CopyOnWriteArrayList<>();
    }

    public synchronized void append(TranslationSegmentEntity segment) {
        segments.add(segment);
        newSegmentCount++;
        while (segments.size() > maxSize) {
            segments.remove(0);
        }
    }

    public List<TranslationSegmentEntity> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public List<TranslationSegmentEntity> getLastN(int n) {
        if (n <= 0 || segments.isEmpty()) {
            return Collections.emptyList();
        }
        int start = Math.max(0, segments.size() - n);
        return new ArrayList<>(segments.subList(start, segments.size()));
    }

    public synchronized void updateCorrection(String segmentId, String correctedSource,
                                               String correctedTarget) {
        for (TranslationSegmentEntity seg : segments) {
            if (seg.getSegmentId().equals(segmentId)) {
                seg.setSourceText(correctedSource);  // Need setter or direct field access
                seg.setTargetText(correctedTarget);
                seg.setStatus(SegmentStatus.CORRECTED);
                seg.setVersion(seg.getVersion() + 1);
                return;
            }
        }
    }

    public synchronized int pollNewSegmentCount() {
        int count = newSegmentCount;
        newSegmentCount = 0;
        return count;
    }

    public synchronized void clear() {
        segments.clear();
        newSegmentCount = 0;
    }

    public int size() {
        return segments.size();
    }
}

