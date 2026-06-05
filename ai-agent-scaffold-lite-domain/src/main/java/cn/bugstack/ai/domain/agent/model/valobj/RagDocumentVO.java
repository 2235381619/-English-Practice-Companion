package cn.bugstack.ai.domain.agent.model.valobj;

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
public class RagDocumentVO {

    private String id;
    private String content;
    private double score;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

}
