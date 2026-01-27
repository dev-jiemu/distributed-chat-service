package com.example.chat.service;

import com.example.chat.model.UserConnection;
import com.example.chat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class ConnectionService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public ConnectionService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @Value("${server.id:server1}")
    private String serverId;
    
    private static final String CONNECTION_KEY_PREFIX = "connection:";
    private static final long CONNECTION_TTL = 30; // 30분
    
    public void saveUserConnection(String userId, String sessionId) {
        // Redis에 연결 정보 저장 (DB 조회 없이)
        UserConnection connection = new UserConnection(userId, sessionId, serverId, System.currentTimeMillis());
        String key = CONNECTION_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, connection, CONNECTION_TTL, TimeUnit.MINUTES);

        log.info("User {} connected to server {} with session {}", userId, serverId, sessionId);
        
        // DB에 사용자가 있으면 활성 시간 업데이트 (선택사항)
        userRepository.findByUserId(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last active time for user: {}", userId);
        });
    }


    public UserConnection getUserConnection(String userId) {
        String key = CONNECTION_KEY_PREFIX + userId;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Retrieved value type from Redis: {}", value != null ? value.getClass().getName() : "null");
            
            if (value == null) {
                return null;
            }
            
            // 이미 UserConnection 타입인 경우
            if (value instanceof UserConnection) {
                return (UserConnection) value;
            }
            
            // LinkedHashMap인 경우 ObjectMapper로 변환
            return objectMapper.convertValue(value, UserConnection.class);
        } catch (Exception e) {
            log.error("Error retrieving user connection for userId: {}", userId, e);
            return null;
        }
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
