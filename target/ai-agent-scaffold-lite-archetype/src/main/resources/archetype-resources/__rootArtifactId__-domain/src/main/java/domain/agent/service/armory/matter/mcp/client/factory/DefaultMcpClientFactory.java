#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.agent.service.armory.matter.mcp.client.factory;

import ${package}.domain.agent.model.valobj.AiAgentConfigTableVO;
import ${package}.domain.agent.service.armory.matter.mcp.client.TooMcpCreateService;
import ${package}.domain.agent.service.armory.matter.mcp.client.impl.LocalToolMcpCreateService;
import ${package}.domain.agent.service.armory.matter.mcp.client.impl.SSEToolMcpCreateService;
import ${package}.domain.agent.service.armory.matter.mcp.client.impl.StdioToolMcpCreateService;
import ${package}.types.enums.ResponseCode;
import ${package}.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class DefaultMcpClientFactory {

    @Resource
    private LocalToolMcpCreateService localToolMcpCreateService;

    @Resource
    private SSEToolMcpCreateService sseToolMcpCreateService;

    @Resource
    private StdioToolMcpCreateService stdioToolMcpCreateService;

    public TooMcpCreateService getTooMcpCreateService(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        if (null != toolMcp.getLocal()) return localToolMcpCreateService;
        if (null != toolMcp.getSse()) return sseToolMcpCreateService;
        if (null != toolMcp.getStdio()) return stdioToolMcpCreateService;
        throw new AppException(ResponseCode.NOT_FOUND_METHOD.getCode(), ResponseCode.NOT_FOUND_METHOD.getInfo());
    }

}
