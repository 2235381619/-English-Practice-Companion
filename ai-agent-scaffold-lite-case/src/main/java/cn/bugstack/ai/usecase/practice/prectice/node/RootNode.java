package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;



/**
 * 根节点 — 设置场景提示词后分发到 ASR 或 LLM 节点
 */
@Slf4j
@Component("PracticeRootNode")
public class RootNode extends AbstractPracticeServiceSupport {

    @Resource
    private ASRNode asrNode;

    @Resource
    private LLMNode llmNode;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity req,
                                     DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        log.info("RootNode: inputType={}, sessionId={}", req.getInputType(), req.getSessionId());
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
