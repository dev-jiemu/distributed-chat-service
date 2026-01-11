package com.example.chat.listener;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.ConnectionService;
import com.example.chat.service.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class StompEventListener {
    private static final Logger log = LoggerFactory.getLogger(StompEventListener.class);

    private final ConnectionService connectionService;
    private final MessagePublisher messagePublisher;
    
    @Value("${app.server-id:server1}")
    private String serverId;

    public StompEventListener(ConnectionService connectionService, MessagePublisher messagePublisher) {
        this.connectionService = connectionService;
        this.messagePublisher = messagePublisher;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("Received a new STOMP connection: {}", headerAccessor.getSessionId());
    }

    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 헤더에서 userId 추출 (실제로는 JWT 토큰에서 추출)
        String userId = extractUserId(headerAccessor);
        
        if (userId != null) {
            // Redis에 연결 정보 저장
            connectionService.saveUserConnection(userId, sessionId);
            
            // 연결 알림 메시지 발송
            ChatMessage joinMessage = ChatMessage.createJoinMessage(userId);
            messagePublisher.publishMessage(joinMessage);
            
            log.info("User {} connected on server {} with session {}", userId, serverId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 세션 속성에서 userId 가져오기
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        
        if (userId != null) {
            // Redis에서 연결 정보 제거
            connectionService.removeUserConnection(userId);
            
            // 연결 해제 알림 메시지 발송
            ChatMessage leaveMessage = ChatMessage.createLeaveMessage(userId);
            messagePublisher.publishMessage(leaveMessage);
            
            log.info("User {} disconnected from server {}", userId, serverId);
        }
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("Subscribe event: sessionId={}, destination={}", 
            headerAccessor.getSessionId(), headerAccessor.getDestination());
    }
    
    private String extractUserId(StompHeaderAccessor headerAccessor) {
        // STOMP 헤더에서 userId 추출
        String userId = headerAccessor.getFirstNativeHeader("userId");
        if (userId != null) {
            return userId;
        }
        
        // 연결 URL 파라미터에서 추출 (폴백)
        if (headerAccessor.getCommand() != null && headerAccessor.getSessionAttributes() != null) {
            Object userIdAttr = headerAccessor.getSessionAttributes().get("userId");
            if (userIdAttr != null) {
                return userIdAttr.toString();
            }
        }
        
        return null;
    }
}
