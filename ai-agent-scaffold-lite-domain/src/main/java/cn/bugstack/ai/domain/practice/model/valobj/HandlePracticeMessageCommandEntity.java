package cn.bugstack.ai.domain.practice.model.valobj;

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
}
