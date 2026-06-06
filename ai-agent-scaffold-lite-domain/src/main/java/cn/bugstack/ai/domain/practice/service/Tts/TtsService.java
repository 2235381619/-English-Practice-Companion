package cn.bugstack.ai.domain.practice.service.Tts;

import cn.bugstack.ai.domain.practice.service.ITtsService;
import cn.xfyun.api.TtsClient;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TTS 服务 — 讯飞 TtsClient (WebSocket 在线语音合成)
 *
 * 注入 IflytekConfiguration 中创建的 TtsClient 单例 Bean。
 * 使用 CountDownLatch 将异步 WebSocket 响应转为同步返回。
 */
@Slf4j
@Service
public class TtsService implements ITtsService {

    private static final long TIMEOUT_SECONDS = 60;

    private final TtsClient ttsClient;

    public TtsService(TtsClient ttsClient) {
        this.ttsClient = ttsClient;
    }

    @Override
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Empty text for TTS");
            return new byte[0];
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        try {
            ttsClient.send(text, new AbstractTtsWebSocketListener() {
                @Override
                public void onSuccess(byte[] bytes) {
                    resultRef.set(bytes);
                    latch.countDown();
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable t, Response response) {
                    errorRef.set(t.getMessage());
                    latch.countDown();
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, TtsResponse response) {
                    errorRef.set("code=" + response.getCode() + " msg=" + response.getMessage());
                    latch.countDown();
                }
            });

            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("TTS synthesize timeout ({}s) for text: {}", TIMEOUT_SECONDS, truncate(text));
                return new byte[0];
            }
        } catch (Exception e) {
            log.error("TTS synthesize failed", e);
            return new byte[0];
        }

        String err = errorRef.get();
        if (err != null) {
            log.error("TTS synthesize error: {}", err);
            return new byte[0];
        }

        byte[] audio = resultRef.get();
        if (audio == null || audio.length == 0) {
            log.warn("TTS returned empty audio for text: {}", truncate(text));
            return new byte[0];
        }

        log.info("TTS synthesized {} bytes for text: {}", audio.length, truncate(text));
        return audio;
    }

    @Override
    public File synthesize(String text, File outputFile) {
        byte[] audio = synthesize(text);
        if (audio.length == 0) {
            return null;
        }

        try {
            // 确保父目录存在
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(audio);
                fos.flush();
            }

            log.info("TTS audio saved to file: {} ({} bytes)", outputFile.getAbsolutePath(), audio.length);
            return outputFile;
        } catch (IOException e) {
            log.error("Failed to write TTS audio to file: {}", outputFile, e);
            return null;
        }
    }

    private static String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
