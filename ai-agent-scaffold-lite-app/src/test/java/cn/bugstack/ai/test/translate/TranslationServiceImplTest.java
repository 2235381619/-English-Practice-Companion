package cn.bugstack.ai.test.translate;

import cn.bugstack.ai.domain.translate.agent.TranscriberAgent;
import cn.bugstack.ai.domain.translate.agent.TranslatorAgent;
import cn.bugstack.ai.domain.translate.model.valobj.TranslateEvent;
import cn.bugstack.ai.domain.translate.service.impl.TranslationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;

/**
 * TranslationServiceImpl 单元测试
 *
 * 模拟 TranscriberAgent 和 TranslatorAgent，验证主流程编排
 */
@Slf4j
public class TranslationServiceImplTest {

    public static void main(String[] args) throws Exception {
        testCreateAndCloseSession();
        testProcessAudioChunkSuccess();
        testProcessAudioChunkSilence();
        testGetEventStream();
        testExportSession();
        testMultipleAudioChunks();
        log.info("===== TranslationServiceImplTest ALL PASSED =====");
    }

    private static void testCreateAndCloseSession() {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);
        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);

        String sessionId = service.createSession("en", "zh");
        assertEq(true, sessionId != null && !sessionId.isEmpty(), "session ID generated");
        assertEq(true, sessionId.length() == 12, "session ID is 12 chars");

        // Close should not throw
        service.closeSession(sessionId);
        log.info("  [PASS] testCreateAndCloseSession");
    }

    private static void testProcessAudioChunkSuccess() throws Exception {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);

        Mockito.when(mockAsr.transcribe(any(), any(), any())).thenReturn("Hello world");
        Mockito.when(mockTl.translate(eq("Hello world"), any(), any(), any()))
                .thenReturn(Flux.just("你好", " ", "世界"));

        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);
        String sessionId = service.createSession("en", "zh");

        List<TranslateEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        service.processAudioChunk(sessionId, 1, "audio-data".getBytes(), "audio/webm")
                .doOnComplete(() -> latch.countDown())
                .subscribe(events::add);

        latch.await(5, TimeUnit.SECONDS);

        assertEq(true, events.size() > 1, "multiple translation events emitted");
        TranslateEvent first = events.get(0);
        assertEq(TranslateEvent.EventType.TRANSLATION, first.getType(), "event type = TRANSLATION");
        assertEq("Hello world", first.getSourceText(), "source text matches ASR output");
        // Last event should be completed (non-partial)
        TranslateEvent last = events.get(events.size() - 1);
        assertEq(false, last.isPartial(), "last event is not partial");

        log.info("  [PASS] testProcessAudioChunkSuccess | events={}", events.size());
    }

    private static void testProcessAudioChunkSilence() throws Exception {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);

        Mockito.when(mockAsr.transcribe(any(), any(), any())).thenReturn("");

        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);
        String sessionId = service.createSession("en", "zh");

        List<TranslateEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        service.processAudioChunk(sessionId, 1, new byte[0], "audio/webm")
                .doOnComplete(() -> latch.countDown())
                .subscribe(events::add);

        latch.await(5, TimeUnit.SECONDS);

        assertEq(1, events.size(), "1 event for silence");
        assertEq(TranslateEvent.EventType.STATUS, events.get(0).getType(), "event type = STATUS");
        assertEq("empty", events.get(0).getStatus(), "status = empty");

        log.info("  [PASS] testProcessAudioChunkSilence");
    }

    private static void testGetEventStream() throws Exception {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);
        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);

        String sessionId = service.createSession("en", "zh");
        List<TranslateEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        service.getEventStream(sessionId).subscribe(event -> {
            events.add(event);
            if (event.getType() == TranslateEvent.EventType.STATUS
                    && "ready".equals(event.getStatus())) {
                latch.countDown();
            }
        });

        // Simulate external event push (like correction service would)
        service.getEventStream(sessionId).subscribe(events::add);
        // We need a way to trigger events. Let's use processAudioChunk instead.
        Mockito.when(mockAsr.transcribe(any(), any(), any())).thenReturn("Test");
        Mockito.when(mockTl.translate(any(), any(), any(), any()))
                .thenReturn(Flux.just("测试"));

        service.processAudioChunk(sessionId, 1, "test".getBytes(), "audio/webm")
                .subscribe();

        Thread.sleep(1000); // Wait for async processing

        assertEq(true, events.size() > 0, "events received via session stream");

        log.info("  [PASS] testGetEventStream | events in stream={}", events.size());
    }

    private static void testExportSession() {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);
        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);

        String sessionId = service.createSession("en", "zh");

        // Add segments via processAudioChunk
        Mockito.when(mockAsr.transcribe(any(), any(), any())).thenReturn("Hello world");
        Mockito.when(mockTl.translate(any(), any(), any(), any()))
                .thenReturn(Flux.just("你好世界"));

        service.processAudioChunk(sessionId, 1, "test".getBytes(), "audio/webm")
                .subscribe();

        // Export
        String export = service.exportSession(sessionId);
        assertEq(true, export != null && !export.isEmpty(), "export is not empty");
        assertEq(true, export.contains("Hello world"), "export contains source text");
        assertEq(true, export.contains("你好世界"), "export contains target text");

        log.info("  [PASS] testExportSession");
    }

    private static void testMultipleAudioChunks() throws Exception {
        TranscriberAgent mockAsr = Mockito.mock(TranscriberAgent.class);
        TranslatorAgent mockTl = Mockito.mock(TranslatorAgent.class);

        Mockito.when(mockAsr.transcribe(any(), any(), any()))
                .thenReturn("First chunk", "Second chunk");
        Mockito.when(mockTl.translate(any(), any(), any(), any()))
                .thenReturn(Flux.just("第一段"), Flux.just("第二段"));

        TranslationServiceImpl service = new TranslationServiceImpl(mockAsr, mockTl);
        String sessionId = service.createSession("en", "zh");

        // Process 2 chunks
        CountDownLatch latch = new CountDownLatch(2);

        service.processAudioChunk(sessionId, 1, "audio1".getBytes(), "audio/webm")
                .doOnComplete(latch::countDown)
                .subscribe();

        service.processAudioChunk(sessionId, 2, "audio2".getBytes(), "audio/webm")
                .doOnComplete(latch::countDown)
                .subscribe();

        latch.await(5, TimeUnit.SECONDS);

        String export = service.exportSession(sessionId);
        assertEq(true, export.contains("First chunk"), "export contains chunk 1 source");
        assertEq(true, export.contains("Second chunk"), "export contains chunk 2 source");
        assertEq(true, export.contains("2 段"), "export counts 2 segments");

        log.info("  [PASS] testMultipleAudioChunks");
    }

    // --- helpers ---

    private static void assertEq(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("[FAIL] %s: expected=%s, actual=%s", msg, expected, actual));
        }
    }
}