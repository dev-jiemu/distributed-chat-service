package com.example.chat.controller;

import com.example.chat.exception.RateLimitExceededException;
import com.example.chat.model.ChatMessage;
import com.example.chat.model.ErrorMessage;
import com.example.chat.service.ConnectionService;
import com.example.chat.service.MessageRoutingService;
import com.example.chat.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
public class ChatController {
    
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    private final MessageRoutingService messageRoutingService;
    private final ConnectionService connectionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimitingService rateLimitingService;

    @Value("${HOSTNAME:server-default}")
    private String serverId;

    public ChatController(MessageRoutingService messageRoutingService, 
                         ConnectionService connectionService,
                         SimpMessagingTemplate messagingTemplate,
                         RateLimitingService rateLimitingService) {
        this.messageRoutingService = messageRoutingService;
        this.connectionService = connectionService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimitingService = rateLimitingService;
    }
    
    /**
     * 채팅 메시지 전송 처리
     * Rate Limiting 적용
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) {
            log.error("Principal is null, cannot process message.");
            throw new IllegalArgumentException("Principal cannot be null");
        }
        String senderId = principal.getName();
        
        try {
            // Rate Limiting 체크 (모든 사용자 동일한 정책)
            rateLimitingService.checkRateLimit(senderId);
        } catch (RateLimitExceededException e) {
            // Rate Limit 초과 - 에러 메시지 전송
            ErrorMessage errorMessage = new ErrorMessage(
                "RATE_LIMIT_EXCEEDED",
                "메시지 전송 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
                e.getRetryAfterSeconds()
            );
            
            messagingTemplate.convertAndSendToUser(
                senderId, 
                "/queue/errors", 
                errorMessage
            );
            
            log.warn("Rate limit exceeded for user: {}", senderId);
            return;  // 메시지 전송 중단
        }
        // ========================================
        
        chatMessage.setSender(senderId);
        chatMessage.setSenderId(senderId);
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setTimestamp(LocalDateTime.now());
        
        log.info("Received message from {} to {}", chatMessage.getSender(), chatMessage.getReceiver());

        // 수신자가 있고 자신이 아닌 경우 메시지 라우팅
        if (chatMessage.getReceiver() != null && !chatMessage.getReceiver().equals(senderId)) {
            messageRoutingService.routeMessage(chatMessage);
        }
        
        // 룸 메시지인 경우 (추후 구현)
        if (chatMessage.getRoomId() != null) {
            // TODO: 룸 멤버들에게 브로드캐스트 (next job)
            log.info("Room message for room: {}", chatMessage.getRoomId());
        }
    }
    
    /**
     * 타이핑 알림
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage message, Principal principal) {
        if (principal == null) {
            log.error("Principal is null, cannot process typing notification.");
            throw new IllegalArgumentException("Principal cannot be null");
        }
        String userId = principal.getName();
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
        if (principal == null) {
            log.error("Principal is null, cannot process read receipt.");
            throw new IllegalArgumentException("Principal cannot be null");
        }
        String userId = principal.getName();
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
