package cn.bugstack.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 讯飞语音配置属性（websdk-java-speech）
 *
 * 只需 APPID + APIKey + APISecret，ASR 和 TTS 共用同一组密钥。
 * 在讯飞开放平台控制台创建应用即可获取。
 */
@Data
@Component
@ConfigurationProperties(prefix = "practice.iflytek")
public class IflytekProperties {
    /** 讯飞控制台 APPID */
    private String appId;
    /** 讯飞 APIKey */
    private String apiKey;
    /** 讯飞 APISecret */
    private String apiSecret;
    /** TTS 发音人: xiaoyan(默认), xiaofeng, catherine 等 */
    private String voiceName = "xiaoyan";
    /** TTS 语速 0-100 */
    private String speed = "50";
    /** TTS 音量 0-100 */
    private String volume = "50";
    /** TTS 音高 0-100 */
    private String pitch = "50";
    /** 音频编码: lame(mp3), raw(pcm), speex 等 */
    private String aue = "lame";
    /** 音频采样率: audio/L16;rate=16000 / audio/L16;rate=8000 */
    private String auf = "audio/L16;rate=16000";
    /** 文本编码: UTF8, GB2312, UNICODE 等 */
    private String tte = "UTF8";
}
