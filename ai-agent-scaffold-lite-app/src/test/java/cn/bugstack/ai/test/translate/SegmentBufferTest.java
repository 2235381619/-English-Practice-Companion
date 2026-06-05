package cn.bugstack.ai.test.translate;

import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;
import cn.bugstack.ai.domain.translate.service.impl.SegmentBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * SegmentBuffer 单元测试
 */
@Slf4j
public class SegmentBufferTest {

    public static void main(String[] args) {
        testAppendAndRetrieve();
        testMaxSizeCulling();
        testUpdateCorrection();
        testPollNewSegmentCount();
        testClear();
        testGetLastNEdgeCases();
        testEmptyBuffer();
        log.info("===== SegmentBufferTest ALL PASSED =====");
    }

    private static void testAppendAndRetrieve() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        for (int i = 1; i <= 5; i++) {
            buffer.append(createSegment("seg_" + i, i, "text_" + i));
        }
        assertEq(5, buffer.size(), "size should be 5");
        assertEq(3, buffer.getLastN(3).size(), "getLastN(3) should return 3");
        assertEq("seg_3", buffer.getLastN(3).get(0).getSegmentId(), "last 3 first id");
        assertEq(5, buffer.getAll().size(), "getAll should return all");
        assertEq("seg_1", buffer.getAll().get(0).getSegmentId(), "first item seg_1");
        log.info("  [PASS] testAppendAndRetrieve");
    }

    private static void testMaxSizeCulling() {
        SegmentBuffer buffer = new SegmentBuffer(3);
        for (int i = 1; i <= 10; i++) {
            buffer.append(createSegment("seg_" + i, i, "text_" + i));
        }
        assertEq(3, buffer.size(), "size should be capped at 3");
        assertEq("seg_8", buffer.getAll().get(0).getSegmentId(), "culled: oldest=seg_8");
        log.info("  [PASS] testMaxSizeCulling");
    }

    private static void testUpdateCorrection() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        buffer.append(createSegment("seg_1", 1, "original text"));
        buffer.updateCorrection("seg_1", "corrected source", "corrected target");
        TranslationSegmentEntity seg = buffer.getLastN(1).get(0);
        assertEq("corrected source", seg.getSourceText(), "source corrected");
        assertEq("corrected target", seg.getTargetText(), "target corrected");
        assertEq(SegmentStatus.CORRECTED, seg.getStatus(), "status = CORRECTED");
        log.info("  [PASS] testUpdateCorrection");
    }

    private static void testPollNewSegmentCount() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        assertEq(0, buffer.pollNewSegmentCount(), "init count = 0");
        buffer.append(createSegment("s1", 1, "a"));
        buffer.append(createSegment("s2", 2, "b"));
        buffer.append(createSegment("s3", 3, "c"));
        assertEq(3, buffer.pollNewSegmentCount(), "3 new segments counted");
        assertEq(0, buffer.pollNewSegmentCount(), "counter reset after poll");
        log.info("  [PASS] testPollNewSegmentCount");
    }

    private static void testClear() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        buffer.append(createSegment("s1", 1, "a"));
        buffer.append(createSegment("s2", 2, "b"));
        buffer.clear();
        assertEq(0, buffer.size(), "cleared: size = 0");
        assertEq(0, buffer.getAll().size(), "cleared: getAll empty");
        assertEq(0, buffer.pollNewSegmentCount(), "cleared: counter reset");
        log.info("  [PASS] testClear");
    }

    private static void testGetLastNEdgeCases() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        // Empty buffer
        assertEq(0, buffer.getLastN(5).size(), "empty: getLastN(5) empty");
        assertEq(0, buffer.getLastN(0).size(), "empty: getLastN(0) empty");
        assertEq(0, buffer.getLastN(-1).size(), "empty: getLastN(-1) empty");
        // Fewer than requested
        buffer.append(createSegment("s1", 1, "a"));
        assertEq(1, buffer.getLastN(10).size(), "getLastN(10) returns 1");
        log.info("  [PASS] testGetLastNEdgeCases");
    }

    private static void testEmptyBuffer() {
        SegmentBuffer buffer = new SegmentBuffer(100);
        assertEq(0, buffer.size(), "initial size = 0");
        assertEq(0, buffer.getAll().size(), "initial getAll empty");
        assertEq(0, buffer.pollNewSegmentCount(), "initial count = 0");
        // updateCorrection on empty buffer should not throw
        buffer.updateCorrection("nonexistent", "src", "tgt");
        assertEq(0, buffer.size(), "update on empty: size still 0");
        log.info("  [PASS] testEmptyBuffer");
    }

    // --- helpers ---

    private static TranslationSegmentEntity createSegment(String id, int seq, String text) {
        TranslationSegmentEntity seg = new TranslationSegmentEntity(id, seq, "en", "zh", text);
        seg.setTargetText("translated_" + text);
        seg.setStatus(SegmentStatus.COMPLETED);
        return seg;
    }

    private static void assertEq(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("[FAIL] %s: expected=%s, actual=%s", msg, expected, actual));
        }
    }
}