package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.ConnectionService;
import com.example.chat.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    
    private final MessageRoutingService messageRoutingService;
    private final ConnectionService connectionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${HOSTNAME:server-default}")
    private String serverId;
    
    /**
     * 채팅 메시지 전송 처리
     * 기존 sendMessage 메서드를 개선하여 에코백 추가
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String senderId = principal != null ? principal.getName() : chatMessage.getSender();
        
        chatMessage.setSender(senderId);
        chatMessage.setSenderId(senderId);
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        
        log.info("Received message from {} to {}", chatMessage.getSender(), chatMessage.getReceiver());
        
        // 발신자에게 에코백 (자신이 보낸 메시지 확인)
        messagingTemplate.convertAndSendToUser(senderId, "/queue/messages", chatMessage);
        
        // 수신자가 있고 자신이 아닌 경우 메시지 라우팅
        if (chatMessage.getReceiver() != null && !chatMessage.getReceiver().equals(senderId)) {
            messageRoutingService.routeMessage(chatMessage);
        }
        
        // 룸 메시지인 경우 (추후 구현)
        if (chatMessage.getRoomId() != null) {
            // TODO: 룸 멤버들에게 브로드캐스트
            log.info("Room message for room: {}", chatMessage.getRoomId());
        }
    }
    
    /**
     * 사용자 접속 처리
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String userId = chatMessage.getSender();
        
        if (sessionId == null) {
            log.error("Session ID is null for user: {}", userId);
            return;
        }
        
        // Redis에 사용자 연결 정보 저장
        connectionService.saveUserConnection(userId, sessionId);
        
        // 세션에 사용자 ID 저장
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("userId", userId);
        } else {
            log.warn("Session attributes is null for session: {}", sessionId);
        }
        
        log.info("User {} joined with session {} on server {}", userId, sessionId, serverId);
        
        // 접속 메시지 생성 (옵션)
        // ChatMessage joinMessage = ChatMessage.createJoinMessage(userId);
        // TODO: 필요시 친구나 룸 멤버들에게 접속 알림 전송
    }
    
    /**
     * 타이핑 알림
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage message, Principal principal) {
        String userId = principal != null ? principal.getName() : message.getSender();
        message.setSender(userId);
        message.setType(ChatMessage.MessageType.TYPING);
        message.setTimestamp(LocalDateTime.now());
        
        // 수신자에게 타이핑 알림 전달
        if (message.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                message.getReceiver(), 
                "/queue/messages", 
                message
            );
        }
    }
    
    /**
     * 읽음 확인
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatMessage message, Principal principal) {
        String userId = principal != null ? principal.getName() : message.getSender();
        message.setSender(userId);
        message.setType(ChatMessage.MessageType.READ);
        message.setTimestamp(LocalDateTime.now());
        
        // 원 발신자에게 읽음 확인 전달
        if (message.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                message.getReceiver(), 
                "/queue/messages", 
                message
            );
        }
    }
}
