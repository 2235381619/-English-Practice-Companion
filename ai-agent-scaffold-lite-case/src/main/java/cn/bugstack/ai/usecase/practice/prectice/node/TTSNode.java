package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TTSNode extends AbstractPracticeServiceSupport {

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity requestParameter,
                                     DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        // TODO: TTS 合成
        // byte[] audio = ttsService.synthesize(dynamicContext.getReplyText());
        // dynamicContext.setAudioUrl("/audio/" + requestParameter.getSessionId() + ".mp3");
        dynamicContext.setSuccess(true);
        log.info("TTSNode: done (placeholder), sessionId={}", requestParameter.getSessionId());
        return PracticeResult.builder()
//                .asrText(dynamicContext.getAsrText())
//                .replyText(dynamicContext.getReplyText())
//                .correctedText(dynamicContext.getCorrectedText())
//                .grammarIssues(dynamicContext.getGrammarIssues())
//                .suggestions(dynamicContext.getSuggestions())
//                .score(dynamicContext.getScore())
//                .audioUrl(dynamicContext.getAudioUrl())
                .build();
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity requestParameter,
            DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
