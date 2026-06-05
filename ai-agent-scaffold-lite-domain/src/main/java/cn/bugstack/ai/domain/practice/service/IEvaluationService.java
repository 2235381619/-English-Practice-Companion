package cn.bugstack.ai.domain.practice.service;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;

/**
 * 评测服务接口 — GPT 语法纠错 + 表达建议 + 评分
 */
public interface IEvaluationService {

    EvaluationResult evaluate(String userText, Scenario scenario, String history);

    EvaluationResult simpleReply(String userText, Scenario scenario);
}
