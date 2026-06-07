package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ISE 发音评测结果值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IseResult {
    /** 总分 0-100 */
    private double totalScore;
    /** 准确度评分 */
    private double accuracyScore;
    /** 流利度评分 */
    private double fluencyScore;
    /** 完整度评分 */
    private double integrityScore;
    /** 原始响应（XML/JSON） */
    private String rawResponse;
    /** 是否成功 */
    private boolean success;
}
