package com.chat.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// TODO : make
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    //private final SessionManager sessionManager;
    //private final MessagePublisher messagePublisher;
    //private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
//        String userId = getUserIdFromSession(session);
//        String serverId = System.getenv("SERVER_ID");

//        sessionManager.registerSession(userId, serverId, session);
//        redisTemplate.opsForHash().put("user:connections", userId, serverId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
//        ChatMessage chatMessage = parseMessage(message.getPayload());
//        chatMessage.setSenderId(getUserIdFromSession(session));
//
//        messagePublisher.publishMessage(chatMessage);
    }
}
