package com.example.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP 메시지 인터셉터
 * CONNECT 시 userId를 Principal로 설정하여 convertAndSendToUser가 작동하도록 함
 */
@Component
public class UserInterceptor implements ChannelInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(UserInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP 헤더에서 userId 추출
            String userId = accessor.getFirstNativeHeader("userId");
            
            if (userId == null) {
                // 세션 속성에서 userId 추출 (WebSocketHandshakeInterceptor에서 설정)
                Object userIdAttr = accessor.getSessionAttributes() != null ? 
                    accessor.getSessionAttributes().get("userId") : null;
                if (userIdAttr != null) {
                    userId = userIdAttr.toString();
                }
            }
            
            if (userId != null) {
                // userId를 Principal로 설정
                final String finalUserId = userId;
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return finalUserId;
                    }
                });
                log.info("Set user principal: {}", userId);
            }
        }
        
        return message;
    }
}
