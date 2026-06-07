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

    /** TTS 音频 Base64 数据 */
    private String audioData;

    /** LLM 建议 **/
    private String suggestion;

    /** 语法纠错后的文本 */
    private String correctedText;

    /** 语法问题列表 */
    private java.util.List<String> grammarIssues;

    /** 表达建议列表 */
    private java.util.List<String> suggestions;

    /** 综合评分 1-10 */
    private int score;

    /** ISE 发音总分 */
    private Double iseTotalScore;

    /** ISE 准确度分 */
    private Double iseAccuracyScore;

    /** ISE 流利度分 */
    private Double iseFluencyScore;

    /** ISE 完整度分 */
    private Double iseIntegrityScore;

//    public static PracticeResult fromContext(PracticeDynamicContext ctx) {
//        return new PracticeResult(ctx.getAsrText(), ctx.getReplyText(), ctx.getAudioUrl());
//    }
}


