package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("PracticeChatModelNode")
public class PracticeChatModelNode extends AbstractPracticeServiceSupport {

    private static final Map<String, String> SCENARIO_PROMPTS = Map.of(
        "default", "You are a friendly English conversation partner. Keep responses natural and concise (1-3 sentences). Just have a natural conversation.",
        "interview", "You are a senior tech interviewer. Ask interview questions, evaluate answers, and provide feedback. Keep responses concise (1-3 sentences).",
        "restaurant", "You are a waiter at a restaurant. Take orders, answer questions about the menu, and make small talk. Keep responses concise (1-3 sentences).",
        "meeting", "You are a business professional in a meeting. Discuss topics professionally, ask questions, and provide feedback. Keep responses concise (1-3 sentences)."
    );

    @Resource
    private ASRNode asrNode;

    @Resource
    private LLMNode llmNode;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        String scenario = ctx.getScenarioCode() != null ? ctx.getScenarioCode() : "default";
        String prompt = SCENARIO_PROMPTS.getOrDefault(scenario, SCENARIO_PROMPTS.get("default"));
        ctx.setSystemPrompt(prompt);
        log.info("ChatModelNode: scenario={}, prompt={}", scenario, prompt);
        return router(req, ctx);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        if (Integer.valueOf(1).equals(req.getInputType())) {
            return asrNode;
        }
        return llmNode;
    }
}
