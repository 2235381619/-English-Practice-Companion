package cn.bugstack.ai.domain.practice.model.entity;

import cn.bugstack.ai.domain.practice.model.valobj.VoiceVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandlePracticeMessageCommandEntity {

    private String sessionId;
    private Integer inputType;
    private byte[] audioData;
    private String text;
    private String scenarioCode;
    /** 语音参数（speed/volume/pitch），可选 */
    private VoiceVo voice;
}
