package cn.bugstack.ai.domain.practice.service;

import cn.bugstack.ai.domain.practice.model.valobj.VoiceVo;
import java.io.File;

/**
 * TTS 服务接口 — 文字转语音
 */
public interface ITtsService {

    /** 文字转语音，使用默认语速/音量/音高 */
    byte[] synthesize(String text);

    /** 文字转语音，可动态调节语音参数 */
    byte[] synthesize(String text, VoiceVo voice);

    /** 文字转语音，直接写入文件，使用默认参数 */
    File synthesize(String text, File outputFile);

    /** 文字转语音，直接写入文件，可动态调节语音参数 */
    File synthesize(String text, File outputFile, VoiceVo voice);
}
