package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 语音参数值对象
 *
 * speed  语速 0-100，默认 50
 * volume 音量 0-100，默认 50
 * pitch  音高 0-100，默认 50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceVo {

    /** 语速 0-100 */
    private int speed = 50;
    /** 音量 0-100 */
    private int volume = 50;
    /** 音高 0-100 */
    private int pitch = 50;

    /** 默认语音参数 */
    public static VoiceVo defaultVoice() {
        return new VoiceVo(50, 50, 50);
    }
}
