package cn.bugstack.ai.domain.iflytek.service;

/**
 * 讯飞实时语音听写服务接口
 *
 * @author bugstack.cn
 */
public interface IIatService {

    /**
     * 初始化服务（已由 IflytekConfiguration 完成，此方法用于显式调用的场景）
     */
    void init() throws Exception;

    /**
     * 将音频数据转写为文字
     *
     * @param audioData 音频字节数组（支持 PCM/WAV/WebM）
     * @param extension 音频格式（"pcm", "wav", "webm" 等）
     * @return 转写文本
     */
    String transcribe(byte[] audioData, String extension) throws Exception;

    /**
     * 关闭服务释放资源
     */
    void shutdown();
}
