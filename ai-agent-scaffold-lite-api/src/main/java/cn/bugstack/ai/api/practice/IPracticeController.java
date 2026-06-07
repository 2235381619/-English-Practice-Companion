package cn.bugstack.ai.api.practice;

import cn.bugstack.ai.api.response.Response;
import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * 口语练习 API 接口
 */
public interface IPracticeController {

    Response<PracticeResult> submitText(HandlePracticeMessageCommandEntity request);

    Response<PracticeResult> submitAudio(HandlePracticeMessageCommandEntity request);

    Response<Void> registerScenario(String sessionId, String scenarioCode);

    Response<Map<String, Object>> getSessionReport(String sessionId);

    ResponseEntity<byte[]> exportSession(String sessionId);
}
