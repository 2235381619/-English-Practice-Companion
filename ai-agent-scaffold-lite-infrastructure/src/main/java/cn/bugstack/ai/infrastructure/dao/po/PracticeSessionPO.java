package cn.bugstack.ai.infrastructure.dao.po;

import lombok.Data;

import java.util.List;

/**
 * 练习会话持久化对象
 */
@Data
public class PracticeSessionPO {

    private String sessionId;
    private String scenarioCode;
    private long createdTime;
    private boolean active;
    private List<ConversationRoundPO> rounds;

    @Data
    public static class ConversationRoundPO {
        private int roundNumber;
        private String userInput;
        private String asrText;
        private String aiReply;
        private int score;
        private String correctedText;
        private List<String> grammarIssues;
        private List<String> suggestions;
        private long timestamp;
    }
}
