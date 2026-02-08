#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.agent.service.armory;

import ${package}.domain.agent.model.entity.ArmoryCommandEntity;
import ${package}.domain.agent.model.valobj.AiAgentConfigTableVO;
import ${package}.domain.agent.model.valobj.AiAgentRegisterVO;
import ${package}.domain.agent.service.IArmoryService;
import ${package}.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class ArmoryService implements IArmoryService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Override
    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception {
        for (AiAgentConfigTableVO table : tables) {
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> handler = defaultArmoryFactory.armoryStrategyHandler();
            handler.apply(
                    ArmoryCommandEntity.builder()
                            .aiAgentConfigTableVO(table)
                            .build(),
                    new DefaultArmoryFactory.DynamicContext());
        }
    }

}
