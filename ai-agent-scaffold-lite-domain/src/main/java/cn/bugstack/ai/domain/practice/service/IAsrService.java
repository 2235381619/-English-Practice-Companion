package cn.bugstack.ai.domain.practice.service;

import java.io.File;

/**
 * ASR 语音识别接口
 * 入参: 麦克风 PCM16 16kHz 单声道音频数据
 * 出参: 识别后的文本，供大模型使用
 */
public interface IAsrService {

    /**
     * 语音识别
     * @param audioData PCM16 16kHz 单声道音频字节数组
     * @return 识别文本，失败返回空字符串
     */
    String transcribe(byte[] audioData);
    String transcribe(File audioFile);
}
