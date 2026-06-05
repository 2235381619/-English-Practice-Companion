package cn.bugstack.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryEntity {

    private String userId;
    private String query;
    private Integer topK;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

}
