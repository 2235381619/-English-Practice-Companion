package cn.bugstack.ai.usecase.practice;

import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.model.valobj.SessionReport;

/**
 * 口语练习用例接口 — 用例层编排入口
 */
public interface IPracticeService {

    /** 创建新练习会话 */
    PracticeSession createSession(Scenario scenario);

    /** 获取会话 */
    PracticeSession getSession(String sessionId);

    /** 处理用户音频（ASR + 评测 + TTS），返回评估结果 */
    EvaluationResult processAudio(String sessionId, byte[] audioData);

    /** 处理已转写的文本 */
    EvaluationResult processText(String sessionId, String text);

    /** 获取课后报告 */
    SessionReport getReport(String sessionId);

    /** 关闭会话 */
    void closeSession(String sessionId);
}
