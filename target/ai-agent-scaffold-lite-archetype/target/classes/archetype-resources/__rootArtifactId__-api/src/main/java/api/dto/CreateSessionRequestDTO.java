#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto;

import lombok.Data;

@Data
public class CreateSessionRequestDTO {

    private String agentId;

    private String userId;

}
