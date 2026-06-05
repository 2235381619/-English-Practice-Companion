package cn.bugstack.ai.domain.practice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import cn.bugstack.ai.domain.practice.service.IAliyunAsrService;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云 NLS 实时语音识别服务
 *
 * 替代 OpenAI Whisper，使用阿里云智能语音交互（NLS）的实时识别 SDK。
 * 支持 PCM/WAV/WebM 输入，自动转换音频格式，返回转写文本。
 */
@Slf4j
public class AliyunAsrService implements IAliyunAsrService {

    private static final String DEFAULT_GATEWAY = "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1";
    private static final String TOKEN_API = "https://nls-meta.cn-shanghai.aliyuncs.com/";
    private static final String FFMPEG = "C:\\ffmpge\\bin\\ffmpeg.exe";

    private final String appKey;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String gatewayUrl;

    private NlsClient client;
    private String currentToken;
    private long tokenExpireTime;

    public AliyunAsrService(String appKey, String accessKeyId, String accessKeySecret) {
        this(appKey, accessKeyId, accessKeySecret, DEFAULT_GATEWAY);
    }

    public AliyunAsrService(String appKey, String accessKeyId, String accessKeySecret, String gatewayUrl) {
        this.appKey = appKey;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.gatewayUrl = gatewayUrl;
    }

    public synchronized void init() throws Exception {
        refreshToken();
        this.client = new NlsClient(gatewayUrl, currentToken);
        log.info("Aliyun NLS client initialized, gateway: {}", gatewayUrl);
    }

    // ===== Token Management =====

    private synchronized void refreshToken() throws Exception {
        if (currentToken != null && System.currentTimeMillis() < tokenExpireTime - 60000) {
            return; // token still valid
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String ts = df.format(new Date());

        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", "CreateToken");
        params.put("Format", "JSON");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", ts);
        params.put("Version", "2019-02-28");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append("&").append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
        }

        String qs = sb.substring(1);
        String sts = "POST&" + urlEncode("/") + "&" + urlEncode(qs);

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes("UTF-8"), "HmacSHA1"));
        String sig = Base64.getEncoder().encodeToString(mac.doFinal(sts.getBytes("UTF-8")));

        String fullUrl = TOKEN_API + "?" + qs + "&Signature=" + urlEncode(sig);

        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write("{}".getBytes("UTF-8"));

        String resp = new String(conn.getInputStream().readAllBytes(), "UTF-8");
        JSONObject json = JSONObject.parseObject(resp);
        JSONObject tokenObj = json.getJSONObject("Token");
        this.currentToken = tokenObj.getString("Id");
        this.tokenExpireTime = System.currentTimeMillis() + 10 * 60 * 1000; // 10 min validity
        log.info("NLS token refreshed");
    }

    // ===== Public API =====

    /**
     * 转写音频数据
     *
     * @param audioData 原始音频 bytes
     * @param extension 格式（"pcm", "wav", "webm"）
     * @return 转写文本
     */
    public String transcribe(byte[] audioData, String extension) throws Exception {
        File pcmFile = null;
        try {
            if ("pcm".equalsIgnoreCase(extension)) {
                pcmFile = writeTempFile(audioData, ".pcm");
            } else {
                pcmFile = convertToPcm(audioData, extension);
            }
            return transcribeFile(pcmFile);
        } finally {
            if (pcmFile != null && pcmFile.exists()) pcmFile.delete();
        }
    }

    public void shutdown() {
        if (client != null) {
            try { client.shutdown(); } catch (Exception e) { log.warn("NLS shutdown: {}", e.getMessage()); }
        }
    }

    // ===== Core Transcription =====

    private String transcribeFile(File pcmFile) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder text = new StringBuilder();

        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            @Override public void onTranscriptionResultChange(SpeechTranscriberResponse r) {}
            @Override public void onTranscriberStart(SpeechTranscriberResponse r) { log.debug("Transcriber start"); }
            @Override public void onSentenceBegin(SpeechTranscriberResponse r) { log.debug("Sentence begin"); }

            @Override
            public void onSentenceEnd(SpeechTranscriberResponse r) {
                if (r.getStatus() == 20000000) {
                    String t = r.getTransSentenceText();
                    if (t != null && !t.isEmpty()) {
                        text.append(t).append(" ");
                    }
                }
            }

            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse r) {
                log.debug("Transcription complete, status: {}", r.getStatus());
                latch.countDown();
            }

            @Override
            public void onFail(SpeechTranscriberResponse r) {
                log.warn("Transcription failed: {} {}", r.getStatus(), r.getStatusText());
                text.append("[ERR:").append(r.getStatus()).append("]");
                latch.countDown();
            }
        };

        SpeechTranscriber transcriber = new SpeechTranscriber(client, listener);
        try {
            transcriber.setAppKey(appKey);
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnableIntermediateResult(false);
            transcriber.setEnablePunctuation(true);
            transcriber.setEnableITN(false);
            transcriber.start();

            byte[] buf = new byte[3200];
            try (FileInputStream fis = new FileInputStream(pcmFile)) {
                int len;
                while ((len = fis.read(buf)) > 0) {
                    transcriber.send(buf, len);
                    Thread.sleep(calcSleep(len, 16000));
                }
            }

            transcriber.stop();
            boolean done = latch.await(30, TimeUnit.SECONDS);
            if (!done) log.warn("Transcription timeout");

        } finally {
            try { transcriber.close(); } catch (Exception ignored) {}
        }

        String result = text.toString().trim();
        if (result.startsWith("[ERR:")) return null;
        return result.isEmpty() ? null : result;
    }

    // ===== Audio Format Conversion =====

    private File convertToPcm(byte[] data, String ext) throws Exception {
        File input = writeTempFile(data, "." + ext);
        File output = File.createTempFile("nls_", ".pcm");
        try {
            boolean hasFfmpeg = new File(FFMPEG).exists();
            if (!hasFfmpeg) {
                throw new RuntimeException("FFmpeg not found at " + FFMPEG + ", cannot convert " + ext + " to PCM");
            }
            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG, "-y", "-i", input.getAbsolutePath(),
                    "-ar", "16000", "-ac", "1", "-f", "s16le",
                    output.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // Read ffmpeg output to avoid deadlock
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (br.readLine() != null) { /* drain */ }
            }
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("FFmpeg exit code " + code);
            }
            return output;
        } finally {
            input.delete();
        }
    }

    private File writeTempFile(byte[] data, String suffix) throws IOException {
        File f = File.createTempFile("nls_", suffix);
        Files.write(f.toPath(), data);
        return f;
    }

    // ===== Utilities =====

    private static int calcSleep(int dataSize, int sampleRate) {
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }

    private static String urlEncode(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
