package cn.bugstack.ai.domain.practice.service.impl;

import cn.bugstack.ai.domain.practice.service.IAliyunAsrService;

import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 科大讯飞 IAT 语音听写服务 — 替代阿里云 NLS
 *
 * 使用 iFLYTEK WebSocket 实时语音识别 SDK。
 * 支持 PCM/WAV/WebM 输入，自动转 PCM 16kHz 16bit mono。
 */
@Slf4j
public class XfyunAsrService implements IAliyunAsrService {

    private static final String FFMPEG = "C:\\ffmpge\\bin\\ffmpeg.exe";

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final int vadEos;

    private IatClient client;

    public XfyunAsrService(String appId, String apiKey, String apiSecret) {
        this(appId, apiKey, apiSecret, 6000);
    }

    public XfyunAsrService(String appId, String apiKey, String apiSecret, int vadEos) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.vadEos = vadEos;
    }

    @Override
    public void init() throws Exception {
        this.client = new IatClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .dwa("wpgs")
                .vad_eos(vadEos)
                .build();
        log.info("iFLYTEK IAT client initialized, VAD eos: {}ms", vadEos);
    }

    @Override
    public String transcribe(byte[] audioData, String extension) throws Exception {
        File pcmFile = null;
        try {
            // Convert to PCM if needed
            if ("pcm".equalsIgnoreCase(extension)) {
                pcmFile = saveTempFile(audioData, ".pcm");
            } else {
                pcmFile = convertToPcm(audioData, extension);
            }
            return transcribePcm(pcmFile);
        } finally {
            if (pcmFile != null && pcmFile.exists()) pcmFile.delete();
        }
    }

    @Override
    public void shutdown() {
        if (client != null) {
            try { client.closeWebsocket(); } catch (Exception ignored) {}
        }
    }

    // ===== Core Transcription =====

    private String transcribePcm(File pcmFile) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder result = new StringBuilder();

        client.send(pcmFile, new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse response) {
                if (response.getCode() != 0) {
                    log.warn("iFLYTEK error: code={}, message={}", response.getCode(), response.getMessage());
                    return;
                }
                if (response.getData() != null && response.getData().getResult() != null) {
                    IatResult iatResult = response.getData().getResult();
                    Text text = iatResult.getText();
                    if (text != null && !text.isDeleted()) {
                        result.append(text.getText());
                    }
                }
                if (response.getData() != null && response.getData().getStatus() == 2) {
                    latch.countDown();
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                log.warn("iFLYTEK transcription failed: {}", t.getMessage());
                latch.countDown();
            }
        });

        boolean done = latch.await(30, TimeUnit.SECONDS);
        if (!done) log.warn("iFLYTEK transcription timeout");

        String text = result.toString().trim();
        return text.isEmpty() ? null : text;
    }

    // ===== Audio Conversion =====

    private File convertToPcm(byte[] data, String ext) throws Exception {
        File input = saveTempFile(data, "." + ext);
        File output = File.createTempFile("xfyun_", ".pcm");
        try {
            if (!new File(FFMPEG).exists()) {
                throw new RuntimeException("FFmpeg not found at " + FFMPEG);
            }
            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG, "-y", "-i", input.getAbsolutePath(),
                    "-ar", "16000", "-ac", "1", "-f", "s16le",
                    output.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (br.readLine() != null) { /* drain */ }
            }
            int code = p.waitFor();
            if (code != 0) throw new RuntimeException("FFmpeg exit code " + code);
            return output;
        } finally {
            input.delete();
        }
    }

    private File saveTempFile(byte[] data, String suffix) throws IOException {
        File f = File.createTempFile("xfyun_", suffix);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
        return f;
    }
}