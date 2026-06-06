package cn.bugstack.ai.usecase.practice;

import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.domain.practice.model.valobj.SessionReport;
import java.io.File;

/**
 * 鍙ｈ缁冧範鐢ㄤ緥鎺ュ彛 鈥?鐢ㄤ緥灞傜紪鎺掑叆鍙?
 */
public interface IPracticeService {


    PracticeSession createSession(Scenario scenario);


    PracticeSession getSession(String sessionId);

    EvaluationResult processAudio(String sessionId, File audioFile);


    EvaluationResult processAudio(String sessionId, byte[] audioData);


    EvaluationResult processText(String sessionId, String text);


    SessionReport getReport(String sessionId);


    void closeSession(String sessionId);
}
