package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.ConnectionService;
import com.example.chat.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    
    private final MessageRoutingService messageRoutingService;
    private final ConnectionService connectionService;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        
        log.info("Received message from {} to {}", chatMessage.getSender(), chatMessage.getReceiver());
        
        // 메시지를 수신자가 있는 서버로 라우팅
        messageRoutingService.routeMessage(chatMessage);
    }
    
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage,
                       SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String userId = chatMessage.getSender();
        
        if (sessionId == null) {
            log.error("Session ID is null for user: {}", userId);
            return;
        }
        
        // Redis에 사용자 연결 정보 저장
        connectionService.saveUserConnection(userId, sessionId);
        
        // 세션에 사용자 ID 저장 (안전한 방식)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("userId", userId);
        } else {
            log.warn("Session attributes is null for session: {}", sessionId);
        }
        
        log.info("User {} joined with session {}", userId, sessionId);
    }
}
