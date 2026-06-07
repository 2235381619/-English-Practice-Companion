package cn.bugstack.ai.usecase.practice.prectice;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.IPracticeService2;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 口语练习服务实现 — 通过规则树编排 ASR → LLM → TTS
 */
@Slf4j
@Service
public class PracticeService2 implements IPracticeService2 {

    @Resource
    private DefaultPracticeFactory defaultPracticeFactory;

    @Override
    public PracticeResult handleMessage(HandlePracticeMessageCommandEntity request) throws Exception {
        long start = System.currentTimeMillis();
        DefaultPracticeFactory.DynamicContext ctx = new DefaultPracticeFactory.DynamicContext();
                ctx.setSessionId(request.getSessionId());
        // 文本模式：将 req.text 设为 asrText
        if (request.getText() != null && !request.getText().isBlank()) {
            ctx.setAsrText(request.getText());
        }

        StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> handler =
                defaultPracticeFactory.strategyHandler();
        PracticeResult result = handler.apply(request, ctx);

        log.info("PracticeService2: asrText=\"{}\", replyText=\"{}\"", result.getAsrText(), result.getReplyText());
        log.info("PracticeService2: totalCost={}ms", System.currentTimeMillis() - start);

        return result;
    }
}
