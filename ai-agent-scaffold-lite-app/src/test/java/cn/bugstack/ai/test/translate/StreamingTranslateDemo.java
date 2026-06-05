package cn.bugstack.ai.test.translate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.FileSystemResource;
import org.junit.Test;
import java.io.*;
import java.util.Arrays;

@Slf4j
public class StreamingTranslateDemo {

    static final String BASE_URL = "https://apis.itedus.cn";
    static final String API_KEY = "sk-BPQbnWSOzxEjOxg9627aFfD87aC44b238eC99c0c992dDd94";
    static final String FFMPEG = "C:\\ffmpge\\bin\\ffmpeg.exe";
    static final String CAPTURE = "C:\\code\\ai-agent-scaffold-lite\\capture.exe";

    static final int SR = 48000, CH = 2, BYTE_PER_SEC = SR * CH * 4;
    static final int CHUNK_SEC = 3, OVLP_SEC = 1, STEP_SEC = CHUNK_SEC - OVLP_SEC;
    static final int CHUNK_B = CHUNK_SEC * BYTE_PER_SEC;
    static final int STEP_B = STEP_SEC * BYTE_PER_SEC;

    @Test
    public void run() throws Exception { main(new String[0]); }

    public static void main(String[] args) throws Exception {
        log.info("=== Streaming Demo ({}s chunks, {}s overlap) ===", CHUNK_SEC, OVLP_SEC);

        var audioApi = OpenAiAudioApi.builder().baseUrl(BASE_URL).apiKey(API_KEY).build();
        var asr = new OpenAiAudioTranscriptionModel(audioApi);
        var chatApi = OpenAiApi.builder().baseUrl(BASE_URL + "/").apiKey(API_KEY)
                .completionsPath("v1/chat/completions").build();
        var translator = OpenAiChatModel.builder().openAiApi(chatApi)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.3d).build())
                .build();

        log.info("Starting capture...");
        Process capture = new ProcessBuilder(CAPTURE, "60", "--pipe").start();
        InputStream pcm = capture.getInputStream();
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(capture.getErrorStream()))) {
                while (r.readLine() != null) {}
            } catch (IOException ignored) {}
        });
        t.setDaemon(true); t.start();

        // 后台线程：持续读 pipe（不丢数据）
        ByteArrayOutputStream ring = new ByteArrayOutputStream();
        Thread captureThread = new Thread(() -> {
            byte[] buf = new byte[131072];
            try {
                while (true) {
                    int n = pcm.read(buf);
                    if (n < 0) break;
                    synchronized (ring) { ring.write(buf, 0, n); }
                }
            } catch (IOException ignored) {}
        });
        captureThread.setDaemon(true);
        captureThread.start();

        log.info("Listening...");
        int seq = 0;
        long chunkStart = System.currentTimeMillis();

        // 主循环：每 500ms 检查是否有足够数据
        try {
            while (capture.isAlive() || captureThread.isAlive()) {
                Thread.sleep(500);
                byte[] snapshot;
                int size;
                synchronized (ring) {
                    size = ring.size();
                    if (size < CHUNK_B) continue;
                    snapshot = ring.toByteArray();
                    // 保留 overlap 部分
                    ring.reset();
                    if (size > STEP_B) ring.write(snapshot, size - STEP_B, STEP_B);
                }

                byte[] chunk = Arrays.copyOf(snapshot, CHUNK_B);
                double wallSec = (System.currentTimeMillis() - chunkStart) / 1000.0;
                chunkStart = System.currentTimeMillis();
                seq++;

                log.info("");
                log.info("--- Chunk #{} ---", seq);

                long t0 = System.currentTimeMillis();
                String text = transcribe(chunk, seq, asr);
                long t1 = System.currentTimeMillis();

                if (text != null && !text.isBlank()) {
                    String zh = translate(text, translator);
                    long t2 = System.currentTimeMillis();
                    log.info("EN: {}", text);
                    log.info("ZH: {}", zh);
                    log.info("ASR {}ms + Trans {}ms", t1 - t0, t2 - t1);
                } else {
                    log.info("(silence)");
                }
            }
        } finally {
            capture.destroy();
        }
        log.info("Done.");
    }

    static String transcribe(byte[] pcm, int seq, OpenAiAudioTranscriptionModel asr) {
        try {
            File raw = File.createTempFile("c" + seq + "_", ".raw");
            raw.deleteOnExit();
            try (FileOutputStream f = new FileOutputStream(raw)) { f.write(pcm); }
            File wav = File.createTempFile("c" + seq + "_", ".wav");
            wav.deleteOnExit();
            ProcessBuilder pb = new ProcessBuilder(FFMPEG, "-y", "-hide_banner", "-loglevel", "error",
                    "-f", "f32le", "-ac", "2", "-ar", "48000",
                    "-i", raw.getAbsolutePath(),
                    "-ac", "1", "-ar", "16000", "-sample_fmt", "s16", wav.getAbsolutePath());
            if (pb.start().waitFor() != 0) return null;
            var opts = OpenAiAudioTranscriptionOptions.builder().model("whisper-1").language("en")
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT).temperature(0.0f).build();
            var resp = asr.call(new AudioTranscriptionPrompt(new FileSystemResource(wav), opts));
            return resp.getResult().getOutput();
        } catch (Exception e) {
            log.warn("ASR err: {}", e.getMessage());
            return null;
        }
    }

    static String translate(String text, OpenAiChatModel translator) {
        try {
            return translator.call("You are a simultaneous interpreter. Translate EN->ZH. " +
                    "Keep terms accurate. Output ONLY translation.\n\nText: " + text);
        } catch (Exception e) {
            log.warn("Translate err: {}", e.getMessage());
            return null;
        }
    }
}