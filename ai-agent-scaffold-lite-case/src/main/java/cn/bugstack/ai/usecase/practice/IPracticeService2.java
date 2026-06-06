package cn.bugstack.ai.usecase.practice;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import org.springframework.http.ResponseEntity;

public interface IPracticeService2 {
    PracticeResult handleMessage(HandlePracticeMessageCommandEntity handlePracticeMessageCommandEntity) throws Exception;
}
