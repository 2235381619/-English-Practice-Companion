package cn.bugstack.ai.api.practice.dto;

import lombok.Data;

@Data
public class CreateSessionResponse {
    private String sessionId;
    private String scenarioCode;
    private String scenarioName;
}
