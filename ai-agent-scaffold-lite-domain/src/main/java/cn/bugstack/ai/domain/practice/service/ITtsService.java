package cn.bugstack.ai.domain.practice.service;

import java.io.IOException;

/**
 * TTS 服务接口 — edge-tts 语音合成
 */
public interface ITtsService {

    void start() throws IOException;

    void speak(String text);

    byte[] synthesize(String text) throws IOException;

    void stop();

    boolean isRunning();
}
