package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 서버의 생존 상태를 주기적으로 체크하는 서비스
 * Key => heartbeat:{serverId} → timestamp (TTL: 15초)
 * interval (갱신 간격): 5초
 * ttl (키 만료): 15초 (interval × 3)
 * threshold (죽은 판단): 10초
 */
@Service
public class ServerHeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(ServerHeartbeatService.class);
    private static final String HEARTBEAT_KEY_PREFIX = "heartbeat:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadPoolTaskScheduler scheduler;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;

    @Value("${app.server-id}")
    private String serverId;

    @Value("${app.heartbeat.interval}")
    private long heartbeatInterval;

    @Value("${app.heartbeat.ttl}")
    private long heartbeatTtl;

    @Value("${app.heartbeat.threshold}")
    private long heartbeatThreshold;

    public ServerHeartbeatService(RedisTemplate<String, Object> redisTemplate, ThreadPoolTaskScheduler scheduler) {
        this.redisTemplate = redisTemplate;
        this.scheduler = scheduler;
    }

    /**
     * 앱 시작 시 하트비트 스케줄링 시작.
     * @Scheduled 대신 수동으로 관리하여 Graceful Shutdown 시 즉시 중지 가능하게 함.
     */
    @jakarta.annotation.PostConstruct
    public void startHeartbeat() {
        scheduledFuture = scheduler.scheduleAtFixedRate(this::publishHeartbeat, Duration.ofSeconds(heartbeatInterval));
        log.info("Heartbeat scheduling started for server: {}, interval: {}s", serverId, heartbeatInterval);
    }

    // 하트비트 갱신
    public void publishHeartbeat() {
        if (stopped.get()) return;

        String key = HEARTBEAT_KEY_PREFIX + serverId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), heartbeatTtl, TimeUnit.SECONDS);
        log.debug("Heartbeat published for server: {}", serverId);
    }

    /**
     * 하트비트 중지 및 Redis 키 즉시 삭제.
     * Graceful Shutdown 시 호출되어, 다른 서버가 빠르게 이 서버를 죽었다고 판단할 수 있게 함.
     */
    public void stopHeartbeat() {
        stopped.set(true);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        String key = HEARTBEAT_KEY_PREFIX + serverId;
        redisTemplate.delete(key);
        log.info("Heartbeat stopped and key deleted for server: {}", serverId);
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
