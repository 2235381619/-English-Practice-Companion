package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.api.practice.dto.*;
import cn.bugstack.ai.api.response.Response;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.types.enums.ResponseCode;
import cn.bugstack.ai.usecase.practice.IPracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 口语练习 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/practice/")
@CrossOrigin(origins = "*")
public class PracticeController {

    @Resource
    private IPracticeService practiceService;

    @PostMapping("session")
    public Response<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        try {
            Scenario scenario = Scenario.fromCode(request.getScenarioCode());
            var session = practiceService.createSession(scenario);

            CreateSessionResponse resp = new CreateSessionResponse();
            resp.setSessionId(session.getSessionId());
            resp.setScenarioCode(scenario.getCode());
            resp.setScenarioName(scenario.getName());

            log.info("Practice session created: id={}, scenario={}", session.getSessionId(), scenario.getCode());
            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(resp).build();
        } catch (Exception e) {
            log.error("Create practice session failed", e);
            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @GetMapping("session/{sessionId}")
    public Response<CreateSessionResponse> getSession(@PathVariable String sessionId) {
        try {
            var session = practiceService.getSession(sessionId);
            if (session == null) {
                return Response.<CreateSessionResponse>builder()
                        .code("0004").info("Session not found").build();
            }
            CreateSessionResponse resp = new CreateSessionResponse();
            resp.setSessionId(session.getSessionId());
            resp.setScenarioCode(session.getScenario().getCode());
            resp.setScenarioName(session.getScenario().getName());
            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(resp).build();
        } catch (Exception e) {
            log.error("Get session failed", e);
            return Response.<CreateSessionResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @PostMapping("audio")
    public Response<SubmitAudioResponse> submitAudio(@RequestParam("sessionId") String sessionId,
                                                      @RequestParam("file") MultipartFile file) {
        try {
            byte[] audioData = file.getBytes();
            var eval = practiceService.processAudio(sessionId, audioData);

            SubmitAudioResponse resp = new SubmitAudioResponse();
            resp.setAsrText(eval.getOriginalText());
            resp.setCorrectedText(eval.getCorrectedText());
            resp.setGrammarIssues(eval.getGrammarIssues());
            resp.setSuggestions(eval.getSuggestions());
            resp.setScore(eval.getScore());
            resp.setAiReply(eval.getAiReply());

            return Response.<SubmitAudioResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(resp).build();
        } catch (Exception e) {
            log.error("Submit audio failed", e);
            return Response.<SubmitAudioResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @PostMapping("text")
    public Response<SubmitAudioResponse> submitText(@RequestParam("sessionId") String sessionId,
                                                     @RequestParam("text") String text) {
        try {
            var eval = practiceService.processText(sessionId, text);

            SubmitAudioResponse resp = new SubmitAudioResponse();
            resp.setAsrText(eval.getOriginalText());
            resp.setCorrectedText(eval.getCorrectedText());
            resp.setGrammarIssues(eval.getGrammarIssues());
            resp.setSuggestions(eval.getSuggestions());
            resp.setScore(eval.getScore());
            resp.setAiReply(eval.getAiReply());

            return Response.<SubmitAudioResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(resp).build();
        } catch (Exception e) {
            log.error("Submit text failed", e);
            return Response.<SubmitAudioResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @GetMapping("session/{sessionId}/report")
    public Response<SessionReportResponse> getReport(@PathVariable String sessionId) {
        try {
            var report = practiceService.getReport(sessionId);
            if (report == null) {
                return Response.<SessionReportResponse>builder()
                        .code("0004").info("Report not found").build();
            }
            SessionReportResponse resp = new SessionReportResponse();
            resp.setSessionId(report.getSessionId());
            resp.setScenarioName(report.getScenario().getName());
            resp.setTotalRounds(report.getTotalRounds());
            resp.setDurationSeconds(report.getDurationSeconds());
            resp.setAverageScore(report.getAverageScore());
            resp.setAllGrammarIssues(report.getAllGrammarIssues());
            resp.setVocabularySuggestions(report.getVocabularySuggestions());
            resp.setRoundSummaries(report.getRoundSummaries());

            return Response.<SessionReportResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(resp).build();
        } catch (Exception e) {
            log.error("Get report failed", e);
            return Response.<SessionReportResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @DeleteMapping("session/{sessionId}")
    public Response<Void> closeSession(@PathVariable String sessionId) {
        try {
            practiceService.closeSession(sessionId);
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).build();
        } catch (Exception e) {
            log.error("Close session failed", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    @GetMapping("scenarios")
    public Response<ScenarioListResponse> listScenarios() {
        try {
            List<ScenarioListResponse.ScenarioItem> items = Arrays.stream(Scenario.values())
                    .map(s -> new ScenarioListResponse.ScenarioItem(s.getCode(), s.getName()))
                    .collect(Collectors.toList());

            ScenarioListResponse data = new ScenarioListResponse();
            data.setScenarios(items);

            return Response.<ScenarioListResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(data).build();
        } catch (Exception e) {
            log.error("List scenarios failed", e);
            return Response.<ScenarioListResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }
}
