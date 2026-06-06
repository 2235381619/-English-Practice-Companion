package cn.bugstack.ai.domain.iflytek.service.impl;

import cn.bugstack.ai.domain.iflytek.service.ITtsService;
import cn.xfyun.api.TtsClient;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 在线语音合成实现
 *
 * 基于 TtsClient WebSocket，参照官方 TtsClientApp 实现。
 * AbstractTtsWebSocketListener(File) 自动将音频写入文件，
 * onClose 后读取文件内容返回。
 *
 * 同时实现 practice.ITtsService 接口，可无缝切换 Edge-TTS。
 */
@Slf4j
public class TtsServiceImpl implements ITtsService {

    private final TtsClient ttsClient;
    private volatile boolean running;

    public TtsServiceImpl(TtsClient ttsClient) {
        this.ttsClient = ttsClient;
    }

    @Override
    public void start() {
        running = true;
        log.info("开启 TTS 引擎已启动");
    }

    @Override
    public void speak(String text) {
        if (!running) {
            log.warn("TTS 引擎未启动");
            return;
        }
        // 服务端场景不做本地播放，只合成返回数据
        try {
            synthesize(text);
        } catch (Exception e) {
            log.warn("TTS speak 失败: {}", e.getMessage());
        }
    }

    @Override
    public byte[] synthesize(String text) throws IOException {
        if (!running) throw new IOException("TTS 引擎未启动");

        CountDownLatch latch = new CountDownLatch(1);
        File tmpFile = File.createTempFile("tts_", ".mp3");
        final Throwable[] error = new Throwable[1];

        try {
            // 参考官方 Demo：AbstractTtsWebSocketListener(file) 自动写文件
            ttsClient.send(text, new AbstractTtsWebSocketListener(tmpFile) {
                @Override
                public void onSuccess(byte[] bytes) {
                    // 父类 AbstractTtsWebSocketListener(file) 已自动写完成文件
                    latch.countDown();
                }

                @Override
                public void onFail(WebSocket ws, Throwable t, Response response) {
                    error[0] = t != null ? t : new IOException("TTS fail: " + response);
                    latch.countDown();
                }

                @Override
                public void onBusinessFail(WebSocket ws, TtsResponse ttsResp) {
                    error[0] = new IOException("TTS business fail: " +
                            (ttsResp != null ? ttsResp.getMessage() : "unknown"));
                    latch.countDown();
                }
            });
        } catch (Exception e) {
            tmpFile.delete();
            throw new IOException("TTS send error", e);
        }

        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                tmpFile.delete();
                throw new IOException("TTS 合成超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tmpFile.delete();
            throw new IOException("TTS 被中断", e);
        }

        if (error[0] != null) {
            tmpFile.delete();
            throw new IOException("TTS 合成失败", error[0]);
        }

        try {
            byte[] result = Files.readAllBytes(tmpFile.toPath());
            log.debug("TTS 合成完成: {} 字节", result.length);
            return result;
        } finally {
            tmpFile.delete();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (ttsClient != null) ttsClient.closeWebsocket();
        log.info("启动 TTS 引擎已经关闭");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
