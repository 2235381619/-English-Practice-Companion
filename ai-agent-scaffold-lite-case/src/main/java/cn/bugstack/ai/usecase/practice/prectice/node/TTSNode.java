package cn.bugstack.ai.usecase.practice.prectice.node;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.domain.practice.model.valobj.VoiceVo;
import cn.bugstack.ai.domain.practice.service.ITtsService;
import cn.bugstack.ai.usecase.practice.prectice.AbstractPracticeServiceSupport;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;

/**
 * TTS 节点 — 将 LLM 回复合成为音频
 *
 * 根据前端传入的 speed/volume/pitch 动态调节语音参数，
 * 合成后的音频文件保存到 audio.output.dir 目录下。
 */
@Slf4j
@Component
public class TTSNode extends AbstractPracticeServiceSupport {

    @Resource
    private ITtsService ttsService;

    @Value("${audio.output.dir:./audio}")
    private String audioOutputDir;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity requestParameter,
                                     DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        String replyText = dynamicContext.getReplyText();

        if (replyText == null || replyText.isBlank()) {
            log.warn("TTSNode: replyText empty, skip TTS, sessionId={}", requestParameter.getSessionId());
            dynamicContext.setSuccess(false);
            return buildResult(dynamicContext);
        }

        // 构建语音参数：前端可选传入，缺省走 50
        VoiceVo voice = new VoiceVo(
                requestParameter.getSpeed()  != null ? requestParameter.getSpeed()  : 50,
                requestParameter.getVolume() != null ? requestParameter.getVolume() : 50,
                requestParameter.getPitch()  != null ? requestParameter.getPitch()  : 50
        );

        // TTS 合成
        byte[] audio = ttsService.synthesize(replyText, voice);
        if (audio.length == 0) {
            log.warn("TTSNode: synthesize returned empty, sessionId={}", requestParameter.getSessionId());
            dynamicContext.setSuccess(false);
            return buildResult(dynamicContext);
        }

        // 保存音频文件
        String audioFileName = requestParameter.getSessionId() + ".mp3";
        File audioDir = new File(audioOutputDir);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        File audioFile = new File(audioDir, audioFileName);
        Files.write(audioFile.toPath(), audio);

        String audioUrl = "/audio/" + audioFileName;
        dynamicContext.setAudioUrl(audioUrl);
        dynamicContext.setSuccess(true);

        log.info("TTSNode: synthesized {} bytes, voice={}, url={}, sessionId={}",
                audio.length, voice, audioUrl, requestParameter.getSessionId());

        return buildResult(dynamicContext);
    }

    private PracticeResult buildResult(DefaultPracticeFactory.DynamicContext ctx) {
        return PracticeResult.builder()
                .asrText(ctx.getAsrText())
                .replyText(ctx.getReplyText())
                .audioUrl(ctx.getAudioUrl())
                .suggestion(String.join("; ", ctx.getSuggestions()))
                .build();
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity requestParameter,
            DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
