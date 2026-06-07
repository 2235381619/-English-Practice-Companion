package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.api.response.Response;
import cn.bugstack.ai.domain.practice.model.entity.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.trigger.listener.PracticeAudioWebSocketHandler;
import java.util.List;
import java.util.Map;
import cn.bugstack.ai.types.enums.ResponseCode;
import cn.bugstack.ai.domain.practice.service.IChatLlmService;
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
            java.util.Map<String, Object> round = new java.util.HashMap<>();
            round.put("asrText", result.getAsrText());
            round.put("replyText", result.getReplyText());
//            round.put("correctedText", result.getCorrectedText());
//            round.put("grammarIssues", result.getGrammarIssues());
//            round.put("suggestions", result.getSuggestions());
//            round.put("score", result.getScore());
            PracticeAudioWebSocketHandler.saveRound(request.getSessionId(), round);
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

    @PostMapping("scenario")
    public Response<Void> registerScenario(@RequestBody HandlePracticeMessageCommandEntity request) {
        try {
            String scenario = request.getScenarioCode() != null ? request.getScenarioCode() : "default";
            String prompt = SCENARIO_PROMPTS.getOrDefault(scenario, SCENARIO_PROMPTS.get("default"));
            chatLlmService.registerSession(request.getSessionId(), prompt);
            log.info("Scenario registered: sessionId={}, scenario={}", request.getSessionId(), scenario);
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("Register scenario failed", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @GetMapping("session/{sessionId}/report")
    public  Response<Map<String, Object>> getSessionReport(@PathVariable String sessionId) {
        try {
            var rounds = cn.bugstack.ai.trigger.listener.PracticeAudioWebSocketHandler.getSessionRounds(sessionId);
            if (rounds == null || rounds.isEmpty()) {
                return Response.<Map<String, Object>>builder()
                        .code("0004").info("No data found").build();
            }

            double avgScore = rounds.stream()
                    .mapToInt(r -> (int) r.getOrDefault("score", 0))
                    .average().orElse(0);
            long totalIssues = rounds.stream()
                    .flatMap(r -> ((List<String>) r.getOrDefault("grammarIssues", List.of())).stream())
                    .count();

            Map<String, Object> report = new java.util.HashMap<>();
            report.put("sessionId", sessionId);
            report.put("totalRounds", rounds.size());
            report.put("averageScore", Math.round(avgScore * 10) / 10.0);
            report.put("totalGrammarIssues", totalIssues);
            report.put("rounds", rounds);

            return Response.<Map<String, Object>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(report)
                    .build();
        } catch (Exception e) {
            log.error("Get report failed", e);
            return Response.<Map<String, Object>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }
}
