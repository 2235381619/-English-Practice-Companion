package cn.bugstack.ai.domain.practice.service.Ise;

import cn.bugstack.ai.config.IflytekProperties;
import cn.xfyun.api.IseClient;
import cn.xfyun.model.response.ise.IseResponseData;
import cn.xfyun.service.ise.AbstractIseWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ISE 发音评测服务 — 基于讯飞 IseClient
 */
@Slf4j
@Service
public class IseService {

    private static final long TIMEOUT_SECONDS = 60;

    private final String appId;
    private final String apiKey;
    private final String apiSecret;

    public IseService(IflytekProperties props) {
        this.appId = props.getAppId();
        this.apiKey = props.getApiKey();
        this.apiSecret = props.getApiSecret();
    }

    public IseResult evaluate(byte[] audioData, String referenceText) {
        if (audioData == null || audioData.length < 64 || referenceText == null || referenceText.isBlank()) {
            log.warn("Invalid ISE params: audio={}, text={}", audioData != null ? audioData.length : 0, referenceText);
            return IseResult.builder().success(false).build();
        }

        IseClient client = new IseClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .addSub("ise")
                .addEnt("en_vip")
                .addCategory("read_sentence")
                .addTte("utf-8")
                .addText('\uFEFF' + referenceText)
                .addRstcd("utf8")
                .addExtraAbility("multi_dimension")
                .addIseUnite("1")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<IseResult> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        try {
            client.send(audioData, (Closeable) null, new AbstractIseWebSocketListener() {
                @Override
                public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
                    try {
                        String raw = new String(
                                Base64.getDecoder().decode(iseResponseData.getData().getData()),
                                StandardCharsets.UTF_8
                        );
                        IseResult result = parseResult(raw);
                        resultRef.set(result);
                    } catch (Exception e) {
                        errorRef.set("Parse error: " + e.getMessage());
                    }
                    latch.countDown();
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable t, Response response) {
                    errorRef.set(t != null ? t.getMessage() : "ISE failed");
                    latch.countDown();
                }
            });

            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("ISE evaluate timeout ({}s)", TIMEOUT_SECONDS);
                return IseResult.builder().success(false).build();
            }
        } catch (Exception e) {
            log.error("ISE evaluate failed", e);
            return IseResult.builder().success(false).build();
        }

        String err = errorRef.get();
        if (err != null) {
            log.error("ISE error: {}", err);
            return IseResult.builder().success(false).build();
        }

        return resultRef.get() != null ? resultRef.get() : IseResult.builder().success(false).build();
    }

    /**
     * 解析 ISE 响应中的分数
     */
    private IseResult parseResult(String raw) {
        log.debug("ISE raw response: {}", raw);

        IseResult.IseResultBuilder builder = IseResult.builder()
                .rawResponse(raw)
                .success(true);

        // 尝试 JSON 解析
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("total_score")) builder.totalScore(json.get("total_score").getAsDouble());
            if (json.has("accuracy_score")) builder.accuracyScore(json.get("accuracy_score").getAsDouble());
            if (json.has("fluency_score")) builder.fluencyScore(json.get("fluency_score").getAsDouble());
            if (json.has("integrity_score")) builder.integrityScore(json.get("integrity_score").getAsDouble());
            return builder.build();
        } catch (Exception ignored) {
            // 非 JSON 格式，尝试 XML 解析
        }

        // XML 解析：匹配 <tag value="数字"/>
        Pattern pattern = Pattern.compile("<(\\w+) value=\"([\\d.]+)\"/>");
        Matcher matcher = pattern.matcher(raw);
        while (matcher.find()) {
            String tag = matcher.group(1);
            double value = Double.parseDouble(matcher.group(2));
            switch (tag) {
                case "total_score" -> builder.totalScore(value);
                case "accuracy_score" -> builder.accuracyScore(value);
                case "fluency_score" -> builder.fluencyScore(value);
                case "integrity_score" -> builder.integrityScore(value);
            }
        }

        return builder.build();
    }
}
