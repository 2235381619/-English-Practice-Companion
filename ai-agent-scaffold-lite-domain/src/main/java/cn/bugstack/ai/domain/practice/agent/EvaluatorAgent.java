package cn.bugstack.ai.domain.practice.agent;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;

/**
 * 基于 LLM 的评测 Agent — 语法纠错 + 表达建议 + 评分
 */
public interface EvaluatorAgent {

    /**
     * 评测用户英语表达
     *
     * @param userText  用户语音转写的文本
     * @param scenario  当前练习场景
     * @param history   历史对话上下文（可选）
     * @return 评测结果
     */
    EvaluationResult evaluate(String userText, Scenario scenario, String history);
}
