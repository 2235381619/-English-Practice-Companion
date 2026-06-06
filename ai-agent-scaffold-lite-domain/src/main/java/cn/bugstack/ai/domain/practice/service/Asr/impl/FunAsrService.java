package cn.bugstack.ai.domain.practice.service.Asr.impl;

import cn.bugstack.ai.domain.practice.service.IAsrService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * FunASR 语音识别实现
 * 通过 HTTP 调用本地 Python FunASR 服务（http://localhost:8765/api/asr/pcm）
 * 接收 PCM16 16kHz 单声道音频，返回识别文本
 */
@Slf4j
@Service
public class FunAsrService implements IAsrService {

    private final HttpClient httpClient;

    @Value("${funasr.url:http://localhost:8765}")
    private String funasrUrl;

    public FunAsrService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String transcribe(byte[] audioData) {
        if (audioData == null || audioData.length < 64) {
            log.warn("Audio data too short: {} bytes", audioData == null ? 0 : audioData.length);
            return "";
        }

        String url = funasrUrl + "/api/asr/pcm";
        log.debug("ASR request to {}, size={} bytes", url, audioData.length);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ASR request failed: HTTP {}", response.statusCode());
                return "";
            }

            String text = JSON.parseObject(response.body()).getString("text");
            if (text == null || text.isBlank()) {
                // 可能返回的是 error 字段
                String error = JSON.parseObject(response.body()).getString("error");
                if (error != null) {
                    log.warn("ASR error: {}", error);
                }
                return "";
            }

            log.info("ASR result: \"{}\"", text);
            return text;

        } catch (Exception e) {
            log.error("ASR request error: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String transcribe(File audioFile) {
        return "";
    }
}
