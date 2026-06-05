package cn.bugstack.ai.api.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ScenarioListResponse {
    private List<ScenarioItem> scenarios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioItem {
        private String code;
        private String name;
    }
}
