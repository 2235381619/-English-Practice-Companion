package cn.bugstack.ai.domain.practice.event;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;

/**
 * 评测结果回调 — domain 层接口，由 trigger 层注册实现
 */
@FunctionalInterface
public interface EvaluationCallback {
    void onEvaluationResult(String sessionId, EvaluationResult eval);
}
