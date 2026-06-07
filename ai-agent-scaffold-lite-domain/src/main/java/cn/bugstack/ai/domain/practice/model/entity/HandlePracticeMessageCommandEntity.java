package cn.bugstack.ai.domain.practice.model.entity;

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
    /** 语速 0-100，默认 50 */
    private Integer speed;
    /** 音量 0-100，默认 50 */
    private Integer volume;
    /** 音高 0-100，默认 50 */
    private Integer pitch;
}
