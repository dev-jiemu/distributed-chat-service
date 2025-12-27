package com.example.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.server-id}")
    private String serverId;
    
    // 로컬 서버의 WebSocket 세션 관리
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    
    private static final String USER_CONNECTION_KEY = "user:connections";
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final long SESSION_TTL_MINUTES = 30;
    
    public void registerSession(String userId, WebSocketSession session) {
        // 로컬 세션 저장
        localSessions.put(userId, session);
        
        // Redis에 사용자 연결 정보 저장
        redisTemplate.opsForHash().put(USER_CONNECTION_KEY, userId, serverId);
        
        // 세션 정보를 TTL과 함께 저장
        String sessionKey = SESSION_KEY_PREFIX + serverId + ":" + userId;
        redisTemplate.opsForValue().set(sessionKey, session.getId(), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        
        log.info("Session registered - UserId: {}, ServerId: {}, SessionId: {}", 
                userId, serverId, session.getId());
    }
    
    public void removeSession(String userId) {
        // 로컬 세션 제거
        localSessions.remove(userId);
        
        // Redis에서 사용자 연결 정보 제거
        redisTemplate.opsForHash().delete(USER_CONNECTION_KEY, userId);
        
        // 세션 정보 제거
        String sessionKey = SESSION_KEY_PREFIX + serverId + ":" + userId;
        redisTemplate.delete(sessionKey);
        
        log.info("Session removed - UserId: {}, ServerId: {}", userId, serverId);
    }
    
    public WebSocketSession getLocalSession(String userId) {
        return localSessions.get(userId);
    }
    
    public String getUserServer(String userId) {
        Object server = redisTemplate.opsForHash().get(USER_CONNECTION_KEY, userId);
        return server != null ? server.toString() : null;
    }
    
    public void sendMessageToLocalUser(String userId, String message) {
        WebSocketSession session = localSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("Message sent to user: {}", userId);
            } catch (IOException e) {
                log.error("Failed to send message to user: {}", userId, e);
            }
        }
    }
    
    public void broadcastToLocalUsers(String message) {
        localSessions.forEach((userId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.debug("Broadcast message sent to user: {}", userId);
                } catch (IOException e) {
                    log.error("Failed to broadcast message to user: {}", userId, e);
                }
            }
        });
    }
    
    public int getLocalSessionCount() {
        return localSessions.size();
    }
    
    public void refreshSession(String userId) {
        String sessionKey = SESSION_KEY_PREFIX + serverId + ":" + userId;
        redisTemplate.expire(sessionKey, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
