package com.example.chat.handler;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.MessagePublisher;
import com.example.chat.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionManager sessionManager;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        log.info("WebSocket connection established. UserId: {}, SessionId: {}", userId, session.getId());
        
        // 세션 등록
        sessionManager.registerSession(userId, session);
        
        // 접속 알림 메시지 발송
        ChatMessage joinMessage = ChatMessage.createJoinMessage(userId);
        messagePublisher.publishMessage(joinMessage);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Message received: {}", payload);
        
        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            String userId = getUserIdFromSession(session);
            chatMessage.setSenderId(userId);
            
            // RabbitMQ로 메시지 발행
            messagePublisher.publishMessage(chatMessage);
            
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        log.info("WebSocket connection closed. UserId: {}, Status: {}", userId, status);
        
        // 세션 제거
        sessionManager.removeSession(userId);
        
        // 종료 알림 메시지 발송
        ChatMessage leaveMessage = ChatMessage.createLeaveMessage(userId);
        messagePublisher.publishMessage(leaveMessage);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for session: {}", session.getId(), exception);
    }
    
    // 세션에서 사용자 ID 추출 (실제로는 인증 토큰에서 추출)
    private String getUserIdFromSession(WebSocketSession session) {
        // TODO: JWT 토큰 파싱 또는 세션 속성에서 가져오기
        // 임시로 쿼리 파라미터에서 가져옴
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return "anonymous-" + session.getId();
    }
}
