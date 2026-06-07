package cn.bugstack.ai.domain.practice.service.Ise;

import cn.bugstack.ai.domain.practice.model.valobj.IseResult;
import cn.bugstack.ai.domain.practice.service.IIseService;
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

@Slf4j
@Service
public class IseServiceImpl implements IIseService {

    private static final long TIMEOUT_SECONDS = 60;

    private final String appId;
    private final String apiKey;
    private final String apiSecret;

    public IseServiceImpl() {
        this.appId = "aa5f53e2";
        this.apiKey = "10ed6197def1ffa8ca32e0ae10c5fc61";
        this.apiSecret = "NGIzMDgyM2RhMTg0NDFkN2MzNjVhNmQx";
    }

    @Override
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
                        resultRef.set(parseResult(raw));
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
                log.warn("ISE timeout ({}s)", TIMEOUT_SECONDS);
                return IseResult.builder().success(false).build();
            }
        } catch (Exception e) {
            log.error("ISE failed", e);
            return IseResult.builder().success(false).build();
        }

        if (errorRef.get() != null) {
            log.error("ISE error: {}", errorRef.get());
            return IseResult.builder().success(false).build();
        }

        IseResult result = resultRef.get();
        return result != null ? result : IseResult.builder().success(false).build();
    }

    private IseResult parseResult(String raw) {
        log.info("ISE raw response: {}", raw);
        IseResult.IseResultBuilder builder = IseResult.builder().rawResponse(raw).success(true);

        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("total_score")) builder.totalScore(json.get("total_score").getAsDouble());
            if (json.has("accuracy_score")) builder.accuracyScore(json.get("accuracy_score").getAsDouble());
            if (json.has("fluency_score")) builder.fluencyScore(json.get("fluency_score").getAsDouble());
            if (json.has("integrity_score")) builder.integrityScore(json.get("integrity_score").getAsDouble());
            return builder.build();
        } catch (Exception ignored) {}

        java.util.Set<String> seen = new java.util.HashSet<>();
        Pattern pattern = Pattern.compile("(\\w+_score)=\\"([\\d.]+)\\"");
        Matcher matcher = pattern.matcher(raw);
        while (matcher.find()) {
            String tag = matcher.group(1);
            double value = Double.parseDouble(matcher.group(2));
            if (seen.contains(tag)) continue;
            seen.add(tag);
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





