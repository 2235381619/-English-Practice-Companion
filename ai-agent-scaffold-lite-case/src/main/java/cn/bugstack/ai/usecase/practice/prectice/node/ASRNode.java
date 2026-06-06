package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeDynamicContext;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.domain.practice.service.IAsrService;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * ASR 节点 — 语音识别 → LLMNode
 */
@Slf4j
@Component
public class ASRNode extends AbstractPracticeServiceSupport {

    @Resource
    private IAsrService asrService;

    @Resource
    private LLMNode llmNode;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity requestParameter, DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        String text = asrService.transcribe(requestParameter.getAudioData());
        dynamicContext.setAsrText(text);
        log.info("ASRNode: result=\"{}\", sessionId={}", text, requestParameter.getSessionId());
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity,  DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity requestParameter, DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        return llmNode;
    }
}
