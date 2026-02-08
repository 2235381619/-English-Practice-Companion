#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.agent.model.valobj.properties;

import ${package}.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AiAgentAutoConfigProperties {

    /**
     * 是否启用AI Agent自动装配
     */
    private boolean enabled = false;

    private Map<String, AiAgentConfigTableVO> tables;

}
