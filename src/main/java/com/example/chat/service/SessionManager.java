package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    private final RedisTemplate<String, Object> redisTemplate;

    public SessionManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${app.server-id}")
    private String serverId;
    
    @Value("${app.connection.max-connections-per-server:1000}")
    private int maxConnectionsPerServer;

    // 로컬 서버의 WebSocket 세션 관리 (userId -> session)
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();

    // 마지막 활동 시간 추적 (userId -> epoch millis)
    private final Map<String, Long> lastActivityTime = new ConcurrentHashMap<>();
    
    private static final String USER_CONNECTION_KEY = "user:connections";
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final long SESSION_TTL_MINUTES = 30;
    
    // 최대 연결 수 초과 여부 체크
    public boolean isConnectionLimitExceeded() {
        boolean exceeded = localSessions.size() >= maxConnectionsPerServer;
        if (exceeded) {
            log.warn("Connection limit exceeded - current: {}, max: {}", localSessions.size(), maxConnectionsPerServer);
        }
        return exceeded;
    }

    public void registerSession(String userId, WebSocketSession session) {
        // 로컬 세션 저장
        localSessions.put(userId, session);
        // 활동시간 저장
        lastActivityTime.put(userId, Instant.now().toEpochMilli());
        
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
        // 활동시간 제거
        lastActivityTime.remove(userId);
        
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

    public Set<String> getAllLocalUserIds() {
        return localSessions.keySet();
    }
    
    public void refreshSession(String userId) {
        String sessionKey = SESSION_KEY_PREFIX + serverId + ":" + userId;
        redisTemplate.expire(sessionKey, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
    }

    // 사용자의 마지막 활동 시간 갱신 (메시지 송수신 시 호출)
    public void updateLastActivity(String userId) {
        lastActivityTime.put(userId, Instant.now().toEpochMilli());
    }

    /**
     * 일정 시간 동안 활동이 없는 idle 세션의 userId 목록 반환
     * @param idleThresholdMillis 기준 시간 (밀리초)
     */
    public Set<String> getIdleUserIds(long idleThresholdMillis) {
        long now = Instant.now().toEpochMilli();
        Set<String> idleUsers = ConcurrentHashMap.newKeySet();

        lastActivityTime.forEach((userId, lastActive) -> {
            if (now - lastActive > idleThresholdMillis) {
                idleUsers.add(userId);
            }
        });

        return idleUsers;
    }
}
