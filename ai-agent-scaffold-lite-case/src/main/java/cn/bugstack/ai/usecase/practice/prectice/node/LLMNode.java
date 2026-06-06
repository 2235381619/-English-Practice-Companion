package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.domain.practice.service.IChatLlmService;
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
        log.info("LLMNode: sessionId={}", req.getSessionId());
        return router(req, ctx);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        return ttsNode;
    }
}
