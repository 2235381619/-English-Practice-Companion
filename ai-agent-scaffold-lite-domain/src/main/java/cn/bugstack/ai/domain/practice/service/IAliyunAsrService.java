package cn.bugstack.ai.domain.practice.service;

/**
 * 阿里云 NLS 实时语音识别服务接口
 */
public interface IAliyunAsrService {

    void init() throws Exception;

    String transcribe(byte[] audioData, String extension) throws Exception;

    void shutdown();
}
