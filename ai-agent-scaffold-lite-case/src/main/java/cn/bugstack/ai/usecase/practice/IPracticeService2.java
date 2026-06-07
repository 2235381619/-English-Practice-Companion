package cn.bugstack.ai.usecase.practice;

import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;

public interface IPracticeService2 {
    PracticeResult handleMessage(HandlePracticeMessageCommandEntity handlePracticeMessageCommandEntity) throws Exception;
}
