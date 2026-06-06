package cn.bugstack.ai.usecase.practice.prectice.node;
import cn.bugstack.ai.usecase.practice.prectice.node.PracticeChatModelNode;
import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("PracticeRootNode")
public class RootNode extends AbstractPracticeServiceSupport {

    @Resource(name = "PracticeChatModelNode")
    private PracticeChatModelNode practiceChatModelNode;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity req,
                                     DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        ctx.setScenarioCode(req.getScenarioCode());
        log.info("RootNode: inputType={}, scenario={}, sessionId={}",
                req.getInputType(), req.getScenarioCode(), req.getSessionId());
        return router(req, ctx);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity req, DefaultPracticeFactory.DynamicContext ctx) throws Exception {
        return practiceChatModelNode;
    }
}
