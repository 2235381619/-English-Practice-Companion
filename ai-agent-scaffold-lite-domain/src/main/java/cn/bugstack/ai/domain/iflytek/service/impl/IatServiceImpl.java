package cn.bugstack.ai.domain.iflytek.service.impl;

import cn.bugstack.ai.domain.iflytek.service.IIatService;
import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.apache.commons.codec.binary.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞实时语音听写实现
 *
 * 基于 IatClient WebSocket，直接将 byte[] 通过 ByteArrayInputStream 送入 SDK，
 * 无需写临时文件，无需 native dll。
 *
 */
@Slf4j
public class IatServiceImpl implements IIatService {

    private static final String FFMPEG = "C:\\ffmpge\\bin\\ffmpeg.exe";

    private final IatClient iatClient;

    public IatServiceImpl(IatClient iatClient) {
        this.iatClient = iatClient;
    }

    @Override
    public void init() {
        log.info("讯飞 IatService 就绪");
    }

    @Override
    public String transcribe(byte[] audioData, String extension) throws Exception {
        // PCM 直接发，其他格式走 ffmpeg 转码
        byte[] pcmData;
        File tmpFile = null;
        if ("pcm".equalsIgnoreCase(extension)) {
            pcmData = audioData;
        } else {
            tmpFile = convertToPcm(audioData, extension);
            pcmData = Files.readAllBytes(tmpFile.toPath());
        }

        try {
            return doTranscribe(pcmData);
        } finally {
            if (tmpFile != null && tmpFile.exists()) tmpFile.delete();
        }
    }

    @Override
    public void shutdown() {
        log.info("讯飞 IatService 已关闭");
    }

    // ===== 核心转写 =====

    private String doTranscribe(byte[] pcmData) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<Text> resultSegments = Collections.synchronizedList(new ArrayList<>());

        AbstractIatWebSocketListener listener = new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse resp) {
                if (resp.getCode() != 0) {
                    log.warn("讯飞 IAT 错误: code={}, msg={}, sid={}", resp.getCode(), resp.getMessage(), resp.getSid());
                    return;
                }
                if (resp.getData() != null && resp.getData().getResult() != null) {
                    IatResult result = resp.getData().getResult();
                    Text text = result.getText();
                    handleResultText(text, resultSegments);
                }
                if (resp.getData() != null && resp.getData().getStatus() == 2) {
                    log.debug("讯飞 IAT 结束, sid={}", resp.getSid());
                    latch.countDown();
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                log.error("讯飞 IAT 连接失败", t);
                latch.countDown();
            }
        };

        // 直接传 InputStream，SDK 内部自动分帧发送
        iatClient.send(new ByteArrayInputStream(pcmData), listener);

        boolean done = latch.await(60, TimeUnit.SECONDS);
        if (!done) log.warn("讯飞 IAT 超时");

        return getFinalResult(resultSegments);
    }

    // ===== 结果处理（参考官方 Demo） =====

    /**
     * 处理流式返回结果（包括全量返回与流式修正替换）
     */
    private void handleResultText(Text text, List<Text> segments) {
        if (StringUtils.equals(text.getPgs(), "rpl")
                && text.getRg() != null && text.getRg().length == 2) {
            int start = text.getRg()[0] - 1;
            int end = text.getRg()[1] - 1;
            for (int i = start; i <= end && i < segments.size(); i++) {
                segments.get(i).setDeleted(true);
            }
        }
        segments.add(text);
    }

    /**
     * 获取最终识别结果（排除被修正替换的片段）
     */
    private static String getFinalResult(List<Text> segments) {
        StringBuilder sb = new StringBuilder();
        for (Text t : segments) {
            if (t != null && !t.isDeleted()) {
                sb.append(t.getText());
            }
        }
        return sb.toString().trim();
    }

    // ===== 音频格式转换 =====

    private File convertToPcm(byte[] data, String ext) throws Exception {
        File input = File.createTempFile("iat_", "." + ext);
        File output = File.createTempFile("iat_", ".pcm");
        Files.write(input.toPath(), data);
        try {
            boolean hasFfmpeg = new File(FFMPEG).exists();
            if (!hasFfmpeg) throw new RuntimeException("FFmpeg not found at " + FFMPEG);
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
            if (p.waitFor() != 0) throw new RuntimeException("FFmpeg 转码失败");
            return output;
        } finally {
            input.delete();
        }
    }
}
