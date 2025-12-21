package com.example.chat.service;

import com.example.chat.model.UserConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor

// TODO : userId 가 not unique 인지 확인해야 할것 같은데
public class ConnectionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${server.id:server1}")
    private String serverId;
    
    private static final String CONNECTION_KEY_PREFIX = "connection:";
    private static final long CONNECTION_TTL = 30; // 30분
    
    public void saveUserConnection(String userId, String sessionId) {
        UserConnection connection = new UserConnection(userId, sessionId, serverId, System.currentTimeMillis());
        String key = CONNECTION_KEY_PREFIX + userId;
        
        redisTemplate.opsForValue().set(key, connection, CONNECTION_TTL, TimeUnit.MINUTES);
        log.info("User {} connected to server {} with session {}", userId, serverId, sessionId);
    }

    public UserConnection getUserConnection(String userId) {
        String key = CONNECTION_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);

        // java.lang.ClassCastException
        // return (UserConnection) redisTemplate.opsForValue().get(key);
        return value == null
                ? null
                : objectMapper.convertValue(value, UserConnection.class);
    }
    
    public void removeUserConnection(String userId) {
        String key = CONNECTION_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("User {} disconnected from server {}", userId, serverId);
    }
    
    public String getUserServer(String userId) {
        UserConnection connection = getUserConnection(userId);
        return connection != null ? connection.getServerId() : null;
    }
}
