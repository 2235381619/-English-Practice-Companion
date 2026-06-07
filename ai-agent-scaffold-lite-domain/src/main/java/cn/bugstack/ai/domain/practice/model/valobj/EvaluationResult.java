package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单轮评测结果 — 语法纠错 + 表达建议 + 评分
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /** 原始用户输入 */
    private String originalText;

    /** 语法纠错（修正后的句子） */
    private String correctedText;

    /** 语法问题列表 */
    private List<String> grammarIssues;

    /** 更好的表达方式建议 */
    private List<String> suggestions;

    /** 综合评分 1-10 */
    private int score;

    /** AI 的自然回复 */
    private String aiReply;
}

    /** ISE 发音总分 */
    private Double iseTotalScore;
    /** ISE 准确度分 */
    private Double iseAccuracyScore;
    /** ISE 流利度分 */
    private Double iseFluencyScore;
    /** ISE 完整度分 */
    private Double iseIntegrityScore;


