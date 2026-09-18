package com.humolix.volur.competition.websocket.session;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;

@Data
@Builder
public class SessionContext implements Serializable {

    private String sessionId;
    private Long userId;
    private String username;
    private Long competitionId;
    private Long teamId;
    private String teamName;
    private String role;
    private String avatar;
    private boolean isReferee;
    private transient WebSocketSession session;

}
