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

import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * TTS 节点 — 将 LLM 回复合成为音频
 *
 * 根据前端传入的 voice.speed/volume/pitch 动态调节语音参数，
 * 合成后的音频以 Base64 格式嵌入响应体，前端可直接播放。
 */
@Slf4j
@Component
public class TTSNode extends AbstractPracticeServiceSupport {

    @Resource
    private ITtsService ttsService;

    @Override
    protected PracticeResult doApply(HandlePracticeMessageCommandEntity requestParameter,
                                     DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        long start = System.currentTimeMillis();
        String replyText = dynamicContext.getReplyText();

        if (replyText == null || replyText.isBlank()) {
            log.warn("TTSNode: replyText empty, skip TTS, sessionId={}", requestParameter.getSessionId());
            dynamicContext.setSuccess(false);
            return buildResult(dynamicContext);
        }

        // 前端传入的语音参数，缺省走默认值
        VoiceVo voice = requestParameter.getVoice();
        if (voice == null) {
            voice = VoiceVo.defaultVoice();
        }

        // TTS 合成
        byte[] audio = ttsService.synthesize(stripMarkdown(replyText), voice);
        if (audio.length == 0) {
            log.warn("TTSNode: synthesize returned empty, sessionId={}", requestParameter.getSessionId());
            dynamicContext.setSuccess(false);
            return buildResult(dynamicContext);
        }

        // 音频转 Base64，嵌入响应体，前端直接播放
        String audioBase64 = Base64.getEncoder().encodeToString(audio);
        dynamicContext.setAudioData(audioBase64);
        dynamicContext.setSuccess(true);

        log.info("TTSNode: synthesized {} bytes, voice={}, sessionId={}, cost={}ms",
                audio.length, voice, requestParameter.getSessionId(), System.currentTimeMillis() - start);

        return buildResult(dynamicContext);
    }

    private PracticeResult buildResult(DefaultPracticeFactory.DynamicContext ctx) {
        return PracticeResult.builder()
                .asrText(ctx.getAsrText())
                .replyText(ctx.getReplyText())
                .audioData(ctx.getAudioData())
                .suggestion(ctx.getSuggestions() != null ? String.join("; ", ctx.getSuggestions()) : "")
                .build();
    }

    @Override
    public StrategyHandler<HandlePracticeMessageCommandEntity, DefaultPracticeFactory.DynamicContext, PracticeResult> get(
            HandlePracticeMessageCommandEntity requestParameter,
            DefaultPracticeFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
    /**
     * 去除文本中的 Markdown 符号，防止 TTS 朗读 ** ` 等标记
     */
    private static String stripMarkdown(String text) {
        if (text == null) return "";
        // **bold** → bold
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        // `code` → code
        text = text.replaceAll("`([^`]+)`", "$1");
        // ~~strikethrough~~ → strikethrough
        text = text.replaceAll("~~(.+?)~~", "$1");
        // [text](url) → text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        // # headers → strip leading #
        text = text.replaceAll("(?m)^#+\\s*", "");
        // - / * list markers → strip
        text = text.replaceAll("(?m)^[-*]\\s+", "");
        return text.trim();
    }

}
