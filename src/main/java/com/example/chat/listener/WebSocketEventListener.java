package com.example.chat.listener;

import com.example.chat.service.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final ConnectionService connectionService;

    public WebSocketEventListener(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            String userId = (String) sessionAttributes.get("userId");
            if (userId != null) {
                log.info("User {} disconnected", userId);
                connectionService.removeUserConnection(userId);
            }
        } else {
            log.debug("Session attributes is null for disconnect event");
        }
    }
}
