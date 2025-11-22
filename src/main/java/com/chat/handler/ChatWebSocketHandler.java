package com.chat.handler;

import com.chat.publish.MessagePublisher;
import com.chat.session.SessionManager;
import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final MessagePublisher messagePublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        String serverId = System.getenv("SERVER_ID");

        sessionManager.registerSession(userId, serverId, session);
        redisTemplate.opsForHash().put("user:connections", userId, serverId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatMessage chatMessage = parseMessage(message.getPayload());
        chatMessage.setSenderId(getUserIdFromSession(session));

        messagePublisher.publishMessage(chatMessage);
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // TODO: JWT 토큰 파싱 또는 세션 속성에서 가져오기
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return "anonymous-" + session.getId();
    }

    private ChatMessage parseMessage(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessage.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message", e);
        }
    }
}
