package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/*
 * IP 기준 WebSocket 연결 시도 Rate Limiting 서비스
 * - 동일 IP에서 단시간에 너무 많은 연결을 맺는 것을 방지
 * - DDoS / 비정상적인 reconnect 루프 차단
 * - 윈도우 내 연결 횟수를 Redis로 카운팅 (Sliding Window 방식)
 */
@Service
public class ConnectionRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionRateLimitService.class);

    private static final String CONN_RATE_KEY_PREFIX = "conn:rate:ip:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.connection.rate-limit.max-attempts:10}")
    private int maxAttempts;      // 윈도우 내 최대 연결 시도 횟수

    @Value("${app.connection.rate-limit.window-seconds:60}")
    private int windowSeconds;    // 윈도우 크기 (초)

    public ConnectionRateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 연결 시도 허용 여부 체크
     * @param ip 클라이언트 IP
     * @return true면 연결 허용, false면 차단
     */
    public boolean allowConnection(String ip) {
        String key = CONN_RATE_KEY_PREFIX + ip;

        // Redis INCR: 값이 없으면 1로 초기화, 있으면 +1
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            log.warn("Redis increment returned null for key: {}", key);
            return true; // Redis 오류 시 일단 허용 (fail-open 정책)
        }

        // 처음 카운트라면 TTL 설정
        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (count > maxAttempts) {
            log.warn("Connection rate limit exceeded - IP: {}, count: {}/{} in {}s window", ip, count, maxAttempts, windowSeconds);
            return false;
        }

        log.debug("Connection attempt allowed - IP: {}, count: {}/{}", ip, count, maxAttempts);
        return true;
    }

    // 현재 IP의 연결 시도 횟수 조회 (모니터링용 ㅇㅂㅇ)
    public long getAttemptCount(String ip) {
        String key = CONN_RATE_KEY_PREFIX + ip;
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }
}
