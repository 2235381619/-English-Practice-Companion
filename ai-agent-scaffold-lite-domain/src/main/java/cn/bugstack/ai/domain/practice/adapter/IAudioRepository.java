package cn.bugstack.ai.domain.practice.adapter;

import java.io.File;

/**
 * 音频文件存储接口
 */
public interface IAudioRepository {

    /** 保存音频文件，返回存储路径 */
    String save(String sessionId, int roundNumber, File audioFile);

    /** 根据路径读取音频文件 */
    File load(String path);
}
