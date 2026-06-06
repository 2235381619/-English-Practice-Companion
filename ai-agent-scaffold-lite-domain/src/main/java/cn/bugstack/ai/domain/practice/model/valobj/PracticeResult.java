package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 口语练习出参 — 树的最终输出
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeResult {

    /** ASR 识别文本 */
    private String asrText;

    /** LLM 回复 */
    private String replyText;

    /** TTS 音频 URL（预留） */
    private String audioUrl;

    /** LLM 建议 **/
    private String suggestion;

//    public static PracticeResult fromContext(PracticeDynamicContext ctx) {
//        return new PracticeResult(ctx.getAsrText(), ctx.getReplyText(), ctx.getAudioUrl());
//    }
}
