package cn.bugstack.ai.domain.practice.service;

import java.io.File;

/**
 * 音频服务接口 — VAD + ASR 转写
 */
public interface IAudioService {

    String transcribe(File wavFile);

    String transcribeFromPcm(byte[] pcmData, int sampleRate);

    String transcribeFromBytes(byte[] audioData, String extension);

    File recordWithVad() throws Exception;

    File recordWithVad(int mixerIdx) throws Exception;
}
