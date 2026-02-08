#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.agent.service.armory.matter.plugin;

import com.google.adk.plugins.LoggingPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("myLogPlugin")
public class MyLogPlugin extends LoggingPlugin {
}
