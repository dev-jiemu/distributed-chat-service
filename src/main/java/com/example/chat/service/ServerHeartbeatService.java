package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 서버의 생존 상태를 주기적으로 체크하는 서비스
 * Key => heartbeat:{serverId} → timestamp (TTL: 15초)
 * interval (갱신 간격): 5초
 * ttl (키 만료): 15초 (interval × 3)
 * threshold (죽은 판단): 10초
 */
@Service
@EnableScheduling
public class ServerHeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(ServerHeartbeatService.class);
    private static final String HEARTBEAT_KEY_PREFIX = "heartbeat:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.server-id}")
    private String serverId;

    @Value("${app.heartbeat.ttl}")
    private long heartbeatTtl;

    @Value("${app.heartbeat.threshold}")
    private long heartbeatThreshold;

    public ServerHeartbeatService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 하트비트 갱신
    @Scheduled(fixedDelayString = "${app.heartbeat.interval}000")
    public void publishHeartbeat() {
        String key = HEARTBEAT_KEY_PREFIX + serverId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), heartbeatTtl, TimeUnit.SECONDS);
        log.debug("Heartbeat published for server: {}", serverId);
    }

    public boolean isServerAlive(String targetServerId) {
        String key = HEARTBEAT_KEY_PREFIX + targetServerId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.warn("Heartbeat key not found for server: {}", targetServerId);
            return false;
        }

        long lastHeartbeat = ((Number) value).longValue();
        long elapsed = (System.currentTimeMillis() - lastHeartbeat) / 1000; // 초 단위

        if (elapsed > heartbeatThreshold) {
            log.warn("Server {} is considered dead. Last heartbeat: {}s ago", targetServerId, elapsed);
            return false;
        }

        return true;
    }
}
