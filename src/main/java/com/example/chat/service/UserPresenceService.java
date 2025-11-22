package com.example.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.server-id}")
    private String serverId;
    
    private static final String PRESENCE_KEY_PREFIX = "presence:";
    private static final String ACTIVE_SERVERS_KEY = "active:servers";
    private static final long PRESENCE_TTL_MINUTES = 5;
    private static final long SERVER_HEARTBEAT_SECONDS = 30;
    
    @PostConstruct
    public void init() {
        // 서버 시작 시 활성 서버로 등록
        registerServerHeartbeat();
    }
    
    public void setUserOnline(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        
        redisTemplate.opsForHash().put(key, "status", "online");
        redisTemplate.opsForHash().put(key, "server", serverId);
        redisTemplate.opsForHash().put(key, "lastSeen", String.valueOf(System.currentTimeMillis()));
        
        redisTemplate.expire(key, PRESENCE_TTL_MINUTES, TimeUnit.MINUTES);
        
        log.info("User presence set online - UserId: {}", userId);
    }
    
    public void setUserOffline(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        
        log.info("User presence set offline - UserId: {}", userId);
    }
    
    public boolean isUserOnline(String userId) {
        return redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId);
    }
    
    public String getUserStatus(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        Object status = redisTemplate.opsForHash().get(key, "status");
        return status != null ? status.toString() : "offline";
    }
    
    // 서버 하트비트 - 30초마다 실행
    @Scheduled(fixedDelay = SERVER_HEARTBEAT_SECONDS * 1000)
    public void registerServerHeartbeat() {
        redisTemplate.opsForSet().add(ACTIVE_SERVERS_KEY, serverId);
        redisTemplate.expire(ACTIVE_SERVERS_KEY, 60, TimeUnit.SECONDS);
        
        log.debug("Server heartbeat registered - ServerId: {}", serverId);
    }
    
    // 죽은 서버의 연결 정리 - 1분마다 실행
    @Scheduled(fixedDelay = 60000)
    public void cleanupDeadConnections() {
        Set<Object> activeServers = redisTemplate.opsForSet().members(ACTIVE_SERVERS_KEY);
        
        // 모든 연결 정보를 조회
        Set<Object> allConnections = redisTemplate.keys("session:*");
        
        if (allConnections != null) {
            for (Object key : allConnections) {
                String keyStr = key.toString();
                // session:serverId:userId 형식에서 serverId 추출
                String[] parts = keyStr.split(":");
                if (parts.length >= 3) {
                    String serverIdFromKey = parts[1];
                    
                    // 활성 서버 목록에 없으면 삭제
                    if (!activeServers.contains(serverIdFromKey)) {
                        redisTemplate.delete(keyStr);
                        log.info("Dead connection cleaned - Key: {}", keyStr);
                    }
                }
            }
        }
    }
}
