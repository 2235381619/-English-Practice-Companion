package cn.bugstack.ai.domain.practice.model.entity;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import lombok.Getter;
import lombok.Setter;

/**
 * 单轮对话实体
 */
@Getter
public class ConversationRound {

    private final int roundNumber;
    private final String userInput;

    @Setter
    private String asrText;

    @Setter
    private String aiReply;

    @Setter
    private EvaluationResult evaluation;

    @Setter
    private long timestamp;

    public ConversationRound(int roundNumber, String userInput) {
        this.roundNumber = roundNumber;
        this.userInput = userInput;
        this.timestamp = System.currentTimeMillis();
    }
}
