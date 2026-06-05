package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课后总结报告
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionReport {

    private String sessionId;
    private Scenario scenario;
    private int totalRounds;
    private int durationSeconds;

    /** 平均评分 */
    private double averageScore;

    /** 所有语法错误汇总 */
    private List<String> allGrammarIssues;

    /** 建议词库（高频建议的表达） */
    private List<String> vocabularySuggestions;

    /** 每个 conversation 的简评列表 */
    private List<String> roundSummaries;
}
