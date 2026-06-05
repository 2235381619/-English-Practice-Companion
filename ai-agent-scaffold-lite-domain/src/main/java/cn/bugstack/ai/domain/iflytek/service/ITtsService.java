package cn.bugstack.ai.domain.iflytek.service;

import java.io.IOException;

/**
 * 讯飞语音合成服务接口
 *
 * @author bugstack.cn
 */
public interface ITtsService {

    /**
     * 启动服务
     */
    void start() throws IOException;

    /**
     * 将文字转为语音并播放
     */
    void speak(String text);

    /**
     * 将文字转为语音，返回音频数据
     */
    byte[] synthesize(String text) throws IOException;

    /**
     * 停止服务
     */
    void stop();

    /**
     * 服务是否运行中
     */
    boolean isRunning();
}
