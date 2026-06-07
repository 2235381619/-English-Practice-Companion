package cn.bugstack.ai.usecase.practice.prectice.factory;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.node.RootNode;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultPracticeFactory {

    @Resource(name = "PracticeRootNode")
    private RootNode rootNode;

    public StrategyHandler<HandlePracticeMessageCommandEntity, DynamicContext, PracticeResult> strategyHandler() {
        return rootNode;
    }

    @Data
    public static class DynamicContext {
        private String sessionId;
        private String scenarioCode;
        private String systemPrompt;
        private String asrText;
        private String replyText;
        private String correctedText;
        private java.util.List<String> grammarIssues = new java.util.ArrayList<>();
        private java.util.List<String> suggestions = new java.util.ArrayList<>();
        private int score;
        private String audioUrl;
        private String audioData;
        private boolean success;
        private String errorMsg;
    }
}
