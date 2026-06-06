package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.service.IChatLlmService;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMNode extends AbstractPracticeServiceSupport {

    @Resource
    private TTSNode ttsNode;

    @Resource
    private IChatLlmService chatLlmService;

    @Resource
    private IEvaluationService evaluationService;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity req,
                                     DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        String asrText = ctx.getAsrText();
        if (asrText == null || asrText.isBlank()) {
            log.warn("LLMNode: asrText empty, sessionId={}", req.getSessionId());
            ctx.setReplyText("");
            ctx.setSuccess(false);
            return router(req, ctx);
        }

        String reply = chatLlmService.chat(asrText, ctx.getSystemPrompt());
        ctx.setReplyText(reply);

        try {
            Scenario scenario = Scenario.fromCode(ctx.getScenarioCode());
            EvaluationResult eval = evaluationService.evaluate(asrText, scenario, "");
            ctx.setCorrectedText(eval.getCorrectedText());
            ctx.setGrammarIssues(eval.getGrammarIssues());
            ctx.setSuggestions(eval.getSuggestions());
            ctx.setScore(eval.getScore());
            log.info("LLMNode: eval score={}, issues={}", eval.getScore(), eval.getGrammarIssues().size());
        } catch (Exception e) {
            log.warn("Evaluation failed: {}", e.getMessage());
        }

        log.info("LLMNode: sessionId={}", req.getSessionId());
        return router(req, ctx);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        return ttsNode;
    }
}
