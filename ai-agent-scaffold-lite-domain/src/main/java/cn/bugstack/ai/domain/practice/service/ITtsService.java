package cn.bugstack.ai.domain.practice.service;

import java.io.File;

/**
 * TTS 服务接口 — 文字转语音
 */
public interface ITtsService {

    /**
     * 文字转语音，返回音频字节数组
     */
    byte[] synthesize(String text);

    /**
     * 文字转语音，直接写入文件
     *
     * @param text      待合成的文本
     * @param outputFile 输出文件路径（如 /tmp/hello.mp3）
     * @return 音频文件，失败返回 null
     */
    File synthesize(String text, File outputFile);
}
