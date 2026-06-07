package cn.bugstack.ai.domain.practice.service.Tts;

import cn.bugstack.ai.domain.practice.model.valobj.VoiceVo;
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
 * 内部维护 TtsConfig 静态类存放固定参数（aue/auf/vcn/tte 等），
 * TtsClient 在 synthesize 方法内按需创建，支持通过 VoiceVo 值对象动态传入 speed/volume/pitch。
 */
@Slf4j
@Service
public class TtsService implements ITtsService {

    private static final long TIMEOUT_SECONDS = 60;

    /**
     * 固定参数配置
     */
    private static class TtsConfig {
        private static final String AUE = "lame";
        private static final String AUF = "audio/L16;rate=16000";
        private static final String VCN = "x4_enus_lucy_education";
        private static final String TTE = "UTF8";
        private static final String REG = "0";
        private static final String RDN = "0";
        private static final String ENT = "intp65_en";
        private static final Integer BGS = 0;
    }

    private final String appId;
    private final String apiKey;
    private final String apiSecret;

    public TtsService() {
        this.appId = "aa5f53e2";
        this.apiKey = "10ed6197def1ffa8ca32e0ae10c5fc61";
        this.apiSecret = "NGIzMDgyM2RhMTg0NDFkN2MzNjVhNmQx";
    }

    private TtsClient buildClient(VoiceVo voice) {
        TtsClient.Builder builder = new TtsClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .aue(TtsConfig.AUE)
                .auf(TtsConfig.AUF)
                .vcn(TtsConfig.VCN)
                .speed(voice.getSpeed())
                .volume(voice.getVolume())
                .pitch(voice.getPitch())
                .tte(TtsConfig.TTE)
                .reg(TtsConfig.REG)
                .rdn(TtsConfig.RDN)
                .ent(TtsConfig.ENT)
                .bgs(TtsConfig.BGS);

        if ("lame".equals(TtsConfig.AUE)) {
            builder.sfl(1);
        }

        TtsClient client = builder.build();
        log.debug("TtsClient 创建 voice={}, speed={}, volume={}, pitch={}",
                TtsConfig.VCN, voice.getSpeed(), voice.getVolume(), voice.getPitch());
        return client;
    }

    @Override
    public byte[] synthesize(String text) {
        return synthesize(text, VoiceVo.defaultVoice());
    }

    @Override
    public byte[] synthesize(String text, VoiceVo voice) {
        if (text == null || text.isBlank()) {
            log.warn("Empty text for TTS");
            return new byte[0];
        }

        TtsClient client = buildClient(voice);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        try {
            client.send(text, new AbstractTtsWebSocketListener() {
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

        log.info("TTS synthesized {} bytes, voice={}", audio.length, voice);
        return audio;
    }

    @Override
    public File synthesize(String text, File outputFile) {
        return synthesize(text, outputFile, VoiceVo.defaultVoice());
    }

    @Override
    public File synthesize(String text, File outputFile, VoiceVo voice) {
        byte[] audio = synthesize(text, voice);
        if (audio.length == 0) {
            return null;
        }

        try {
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
