package cn.bugstack.ai.domain.practice.event;

import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;

/**
 * 评测结果发布器 — domain 层静态持有者
 *
 * trigger 层启动时注册回调，case/domain 层通过 publish 发送评测结果。
 */
public class EvaluationResultPublisher {

    private static EvaluationCallback callback;

    private EvaluationResultPublisher() {
    }

    public static void setCallback(EvaluationCallback callback) {
        EvaluationResultPublisher.callback = callback;
    }

    public static void publish(String sessionId, EvaluationResult eval) {
        if (callback != null) {
            callback.onEvaluationResult(sessionId, eval);
        }
    }
}
