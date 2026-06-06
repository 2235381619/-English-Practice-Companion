package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.api.response.Response;
import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.types.enums.ResponseCode;
import cn.bugstack.ai.usecase.practice.IPracticeService2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/v1/practice/")
@CrossOrigin(origins = "*")
public class PracticeController {

    @Resource
    private IPracticeService2 practiceService2;

    @PostMapping("text")
    public Response<PracticeResult> submitText(@RequestBody HandlePracticeMessageCommandEntity request) {
        try {
            request.setInputType(2);
            PracticeResult result = practiceService2.handleMessage(request);
            log.info("Practice text: sessionId={}, asrText={}, reply={}",
                    request.getSessionId(), result.getAsrText(), result.getReplyText());
            return Response.<PracticeResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("Submit text failed", e);
            return Response.<PracticeResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @PostMapping("session")
    public Response<PracticeResult> submitAudio(@RequestBody HandlePracticeMessageCommandEntity request) {
        try {
            request.setInputType(1);
            PracticeResult result = practiceService2.handleMessage(request);
            log.info("Practice audio: sessionId={}, asrText={}, reply={}",
                    request.getSessionId(), result.getAsrText(), result.getReplyText());
            return Response.<PracticeResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("Submit audio failed", e);
            return Response.<PracticeResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }
}
