package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.service.IChatLlmService;
import cn.bugstack.ai.domain.practice.event.EvaluationResultPublisher;
import cn.bugstack.ai.domain.practice.service.IEvaluationService;
import java.util.concurrent.CompletableFuture;
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
        long start = System.currentTimeMillis();
        String asrText = ctx.getAsrText();
        if (asrText == null || asrText.isBlank()) {
            log.warn("LLMNode: asrText empty, sessionId={}", req.getSessionId());
            ctx.setReplyText("");
            ctx.setSuccess(false);
            return router(req, ctx);
        }

        String reply = chatLlmService.chatBySession(asrText, req.getSessionId());
        ctx.setReplyText(reply);

        // 规则树内异步评测，不阻塞 TTS 合成
        String sessionId = req.getSessionId();
        String scenarioCode = ctx.getScenarioCode();
        CompletableFuture.runAsync(() -> {
            try {
                log.info("LLMEval starting: sessionId={}", sessionId);
                EvaluationResult eval = evaluationService.evaluate(
                        sessionId, asrText, Scenario.fromCode(scenarioCode), ctx.getAudioBytes());
                EvaluationResultPublisher.publish(sessionId, eval);
                log.info("LLMEval done: sessionId={}, score={}", sessionId, eval.getScore());
            } catch (Exception e) {
                log.warn("LLMEval failed: sessionId={}, {}", sessionId, e.getMessage());
            }
        });


        log.info("LLMNode: sessionId={}, cost={}ms", req.getSessionId(), System.currentTimeMillis() - start);
        return router(req, ctx);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        return ttsNode;
    }
}




