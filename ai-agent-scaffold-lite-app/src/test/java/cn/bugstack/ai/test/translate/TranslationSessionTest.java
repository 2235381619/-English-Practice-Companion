package cn.bugstack.ai.test.translate;

import cn.bugstack.ai.domain.translate.model.entity.TranslationSegmentEntity;
import cn.bugstack.ai.domain.translate.model.valobj.SegmentStatus;
import cn.bugstack.ai.domain.translate.model.valobj.TranslateEvent;
import cn.bugstack.ai.domain.translate.service.impl.SegmentBuffer;
import cn.bugstack.ai.domain.translate.service.impl.TranslationSession;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TranslationSession 单元测试
 */
@Slf4j
public class TranslationSessionTest {

    public static void main(String[] args) throws Exception {
        testCreateSession();
        testEmitAndReceiveEvents();
        testEventTypes();
        testCloseSession();
        testExportAsText();
        testNextSegmentId();
        testInactiveSessionDropsEvents();
        log.info("===== TranslationSessionTest ALL PASSED =====");
    }

    private static void testCreateSession() {
        TranslationSession session = new TranslationSession("test-session-1", "en", "zh");
        assertEq("test-session-1", session.getSessionId(), "session ID");
        assertEq("en", session.getSourceLang(), "source lang");
        assertEq("zh", session.getTargetLang(), "target lang");
        assertEq(true, session.isActive(), "active on creation");
        assertEq(0, session.currentSequence(), "initial sequence = 0");
        log.info("  [PASS] testCreateSession");
    }

    private static void testEmitAndReceiveEvents() throws Exception {
        TranslationSession session = new TranslationSession("emit-test", "en", "zh");
        List<TranslateEvent> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        Disposable sub = session.getEventStream().subscribe(event -> {
            received.add(event);
            latch.countDown();
        });

        session.emit(TranslateEvent.status("connected", "hello"));
        session.emit(TranslateEvent.status("ready", "world"));

        assertEq(true, latch.await(2, TimeUnit.SECONDS), "events received within timeout");
        assertEq(2, received.size(), "2 events received");
        assertEq("connected", received.get(0).getStatus(), "first event status");
        assertEq("ready", received.get(1).getStatus(), "second event status");
        sub.dispose();
        log.info("  [PASS] testEmitAndReceiveEvents");
    }

    private static void testEventTypes() throws Exception {
        TranslationSession session = new TranslationSession("types-test", "en", "zh");
        List<TranslateEvent> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        session.getEventStream().subscribe(event -> {
            received.add(event);
            latch.countDown();
        });

        // Emit TRANSLATION event
        TranslationSegmentEntity seg = new TranslationSegmentEntity("seg_001", 1, "en", "zh", "Hello");
        seg.setTargetText("你好");
        seg.setStatus(SegmentStatus.COMPLETED);
        session.emit(TranslateEvent.translation(seg, false));

        // Emit CORRECTION event
        session.emit(TranslateEvent.correction("seg_001", 1, "Hello world", "你好世界", "ASR fix"));

        latch.await(2, TimeUnit.SECONDS);

        TranslateEvent evt1 = received.get(0);
        assertEq(TranslateEvent.EventType.TRANSLATION, evt1.getType(), "event type = TRANSLATION");
        assertEq("Hello", evt1.getSourceText(), "translation source");
        assertEq("你好", evt1.getTargetText(), "translation target");

        TranslateEvent evt2 = received.get(1);
        assertEq(TranslateEvent.EventType.CORRECTION, evt2.getType(), "event type = CORRECTION");
        assertEq("seg_001", evt2.getSegmentId(), "correction segment ID");
        assertEq("Hello world", evt2.getCorrectedSource(), "corrected source");
        log.info("  [PASS] testEventTypes");
    }

    private static void testCloseSession() {
        TranslationSession session = new TranslationSession("close-test", "en", "zh");
        assertEq(true, session.isActive(), "active before close");
        session.close();
        assertEq(false, session.isActive(), "inactive after close");
        log.info("  [PASS] testCloseSession");
    }

    private static void testExportAsText() {
        TranslationSession session = new TranslationSession("export-test", "en", "zh");
        SegmentBuffer buffer = session.getBuffer();

        TranslationSegmentEntity s1 = new TranslationSegmentEntity("seg_001", 1, "en", "zh", "Hello world");
        s1.setTargetText("你好世界");
        s1.setStatus(SegmentStatus.COMPLETED);
        buffer.append(s1);

        TranslationSegmentEntity s2 = new TranslationSegmentEntity("seg_002", 2, "en", "zh", "Good bye");
        s2.setTargetText("再见");
        s2.setStatus(SegmentStatus.COMPLETED);
        buffer.append(s2);

        String export = session.exportAsText();

        assertEq(true, export.contains("export-test"), "export contains session ID");
        assertEq(true, export.contains("en \u2192 zh"), "export contains lang pair");
        assertEq(true, export.contains("Hello world"), "export contains source text 1");
        assertEq(true, export.contains("你好世界"), "export contains target text 1");
        assertEq(true, export.contains("Good bye"), "export contains source text 2");
        assertEq(true, export.contains("再见"), "export contains target text 2");
        assertEq(true, export.contains("2 段"), "export contains segment count");
        log.info("  [PASS] testExportAsText");
    }

    private static void testNextSegmentId() {
        TranslationSession session = new TranslationSession("seq-test", "en", "zh");
        assertEq("seg_0001", session.nextSegmentId(), "first ID = seg_0001");
        assertEq("seg_0002", session.nextSegmentId(), "second ID = seg_0002");
        assertEq("seg_0003", session.nextSegmentId(), "third ID = seg_0003");
        assertEq(3, session.currentSequence(), "sequence counter = 3");
        log.info("  [PASS] testNextSegmentId");
    }

    private static void testInactiveSessionDropsEvents() throws Exception {
        TranslationSession session = new TranslationSession("inactive-test", "en", "zh");
        List<TranslateEvent> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        session.getEventStream().subscribe(event -> {
            received.add(event);
            latch.countDown();
        });

        session.close(); // Mark inactive
        session.emit(TranslateEvent.status("should", "not arrive"));

        assertEq(false, latch.await(500, TimeUnit.MILLISECONDS), "events should NOT arrive after close");
        assertEq(0, received.size(), "0 events received after close");
        log.info("  [PASS] testInactiveSessionDropsEvents");
    }

    // --- helpers ---

    private static void assertEq(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("[FAIL] %s: expected=%s, actual=%s", msg, expected, actual));
        }
    }
}