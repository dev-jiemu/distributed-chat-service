package com.chat.session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SessionManager {
    private final Map<String, WebSocketSession> localSession = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;

    public void registerSession(String userId, String serverId, WebSocketSession session) {
        localSession.put(userId, session);

        // Redis에 TTL과 함께 저장
        String key = String.format("session:%s:%s", serverId, userId);
        redisTemplate.opsForValue().set(key, session.getId(), 30, TimeUnit.MINUTES);
    }

    public WebSocketSession getLocalSession(String userId) {
        return localSession.get(userId);
    }

    public String getUserServer(String userId) {
        return (String) redisTemplate.opsForHash().get("user:connections", userId);
    }
}
