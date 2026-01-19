package com.example.chat.service;

import com.example.chat.exception.RateLimitExceededException;
import com.example.chat.model.RateLimitInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Token Bucket 알고리즘을 사용한 Rate Limiting 서비스
 * 
 * Token Bucket 작동 방식:
 * 1. 각 사용자는 "토큰 바구니"를 가짐
 * 2. 메시지 1개 = 토큰 1개 소비
 * 3. 토큰은 시간이 지나면 자동으로 충전됨 (refillRate)
 * 4. 바구니는 최대 용량(maxTokens)까지만 담을 수 있음
 * 5. 토큰이 없으면 메시지 전송 불가
 *
 * Ref. https://etloveguitar.tistory.com/126 읽어보기 :)
 */
@Service
public class RateLimitingService {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    
    private static final String RATE_LIMIT_KEY_PREFIX_ANON = "rate:anon:";
    private static final String RATE_LIMIT_KEY_PREFIX_AUTH = "rate:auth:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 설정값
    @Value("${app.rate-limit.anonymous.limit}")
    private int anonymousLimit;  // 분당 메시지 수
    
    @Value("${app.rate-limit.anonymous.burst}")
    private int anonymousBurst;  // 버스트 허용
    
    @Value("${app.rate-limit.authenticated.limit}")
    private int authenticatedLimit;
    
    @Value("${app.rate-limit.authenticated.burst}")
    private int authenticatedBurst;
    
    @Value("${app.rate-limit.token-ttl}")
    private int tokenTtl;  // Redis TTL (초)
    
    // 메트릭
    private final Counter allowedCounter;
    private final Counter rejectedCounter;
    private final Counter anonymousRejectedCounter;
    private final Counter authenticatedRejectedCounter;

    public RateLimitingService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        // Micrometer 메트릭 초기화
        this.allowedCounter = Counter.builder("rate_limit.allowed")
                .description("Number of allowed requests")
                .register(meterRegistry);
        
        this.rejectedCounter = Counter.builder("rate_limit.rejected")
                .description("Number of rejected requests")
                .register(meterRegistry);
        
        this.anonymousRejectedCounter = Counter.builder("rate_limit.rejected.anonymous")
                .description("Number of rejected anonymous requests")
                .register(meterRegistry);
        
        this.authenticatedRejectedCounter = Counter.builder("rate_limit.rejected.authenticated")
                .description("Number of rejected authenticated requests")
                .register(meterRegistry);
    }

    /**
     * Rate Limit 체크 (메인 메서드)
     * 
     * @param userId 사용자 ID (익명의 경우 clientIdentifier)
     * @param isAuthenticated 인증된 사용자 여부
     * @return true면 메시지 전송 가능, false면 불가
     * @throws RateLimitExceededException Rate Limit 초과 시
     */
    public boolean checkRateLimit(String userId, boolean isAuthenticated) {
        String redisKey = getRedisKey(userId, isAuthenticated);
        
        // Redis에서 현재 Rate Limit 정보 조회
        RateLimitInfo rateLimitInfo = getRateLimitInfo(redisKey, isAuthenticated);
        
        // Token Bucket 알고리즘 적용
        LocalDateTime now = LocalDateTime.now();
        double newTokens = calculateNewTokens(rateLimitInfo, now);
        
        // 토큰이 1개 이상 있으면 허용
        if (newTokens >= 1.0) {
            // 토큰 1개 소비
            rateLimitInfo.setTokens(newTokens - 1.0);
            rateLimitInfo.setLastRefillTime(now);
            
            // Redis에 업데이트
            saveRateLimitInfo(redisKey, rateLimitInfo);
            
            // 메트릭 기록
            allowedCounter.increment();
            
            log.debug("Rate limit check passed for user: {} (authenticated: {}), remaining tokens: {}", 
                userId, isAuthenticated, rateLimitInfo.getTokens());
            
            return true;
        } else {
            // 토큰 부족 - 거부
            // 다음 토큰이 충전될 때까지 대기 시간 계산
            long retryAfterSeconds = calculateRetryAfter(rateLimitInfo);
            
            // 메트릭 기록
            rejectedCounter.increment();
            if (isAuthenticated) {
                authenticatedRejectedCounter.increment();
            } else {
                anonymousRejectedCounter.increment();
            }
            
            log.warn("Rate limit exceeded for user: {} (authenticated: {}), retry after {} seconds", 
                userId, isAuthenticated, retryAfterSeconds);
            
            throw new RateLimitExceededException(userId, retryAfterSeconds);
        }
    }

    /**
     * Token Bucket 알고리즘: 새로운 토큰 수 계산
     * 
     * ** 공식: 현재 토큰 = 이전 토큰 + (경과 시간 × 충전 속도) **
     * 단, 최대 용량을 초과할 수 없음
     */
    private double calculateNewTokens(RateLimitInfo info, LocalDateTime now) {
        // 마지막 충전 이후 경과 시간 (초 단위)
        Duration elapsed = Duration.between(info.getLastRefillTime(), now);
        double elapsedSeconds = elapsed.toMillis() / 1000.0;
        
        // 충전된 토큰 수 = 경과 시간 × 초당 충전 비율
        double refillAmount = elapsedSeconds * info.getRefillRate();
        
        // 새로운 토큰 수 = 이전 토큰 + 충전된 토큰
        // 단, 최대 용량을 초과할 수 없음
        double newTokens = Math.min(
            info.getTokens() + refillAmount,
            info.getMaxTokens()
        );
        
        log.debug("Token calculation - Previous: {}, Elapsed: {}s, Refill: {}, New: {}, Max: {}", 
            info.getTokens(), elapsedSeconds, refillAmount, newTokens, info.getMaxTokens());
        
        return newTokens;
    }

    /**
     * 다음 토큰 충전까지 대기 시간 계산
     */
    private long calculateRetryAfter(RateLimitInfo info) {
        // 토큰 1개 충전에 필요한 시간 (초)
        double secondsPerToken = 1.0 / info.getRefillRate();
        
        // 현재 토큰이 0.3개라면, 0.7개 더 필요 → 0.7 / refillRate 초 대기
        double tokensNeeded = 1.0 - info.getTokens();
        long retryAfter = (long) Math.ceil(tokensNeeded * secondsPerToken);
        
        // 최소 1초
        return Math.max(1, retryAfter);
    }

    /**
     * Redis에서 Rate Limit 정보 조회
     * 없으면 새로 생성 (처음 접속한 사용자)
     */
    private RateLimitInfo getRateLimitInfo(String redisKey, boolean isAuthenticated) {
        try {
            String json = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (json != null) {
                // 기존 정보 있음
                return objectMapper.readValue(json, RateLimitInfo.class);
            } else {
                // 처음 접속 - 초기화
                int maxTokens = isAuthenticated ? authenticatedBurst : anonymousBurst;
                double refillRate = calculateRefillRate(isAuthenticated);
                
                return RateLimitInfo.initialize(maxTokens, refillRate);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse RateLimitInfo from Redis", e);
            // 파싱 실패 시 새로 생성
            int maxTokens = isAuthenticated ? authenticatedBurst : anonymousBurst;
            double refillRate = calculateRefillRate(isAuthenticated);
            return RateLimitInfo.initialize(maxTokens, refillRate);
        }
    }

    /**
     * Redis에 Rate Limit 정보 저장
     */
    private void saveRateLimitInfo(String redisKey, RateLimitInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(redisKey, json, tokenTtl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to save RateLimitInfo to Redis", e);
        }
    }

    /**
     * 초당 토큰 충전 비율 계산
     * 
     * 예: 분당 200개 → 초당 200/60 = 3.33개
     */
    private double calculateRefillRate(boolean isAuthenticated) {
        int limitPerMinute = isAuthenticated ? authenticatedLimit : anonymousLimit;
        return limitPerMinute / 60.0;  // 분당 → 초당 변환
    }

    /**
     * Redis 키 생성
     */
    private String getRedisKey(String userId, boolean isAuthenticated) {
        String prefix = isAuthenticated ? RATE_LIMIT_KEY_PREFIX_AUTH : RATE_LIMIT_KEY_PREFIX_ANON;
        return prefix + userId;
    }

    /**
     * 사용자의 Rate Limit 정보 조회 (디버깅/모니터링용)
     */
    public RateLimitInfo getRateLimitStatus(String userId, boolean isAuthenticated) {
        String redisKey = getRedisKey(userId, isAuthenticated);
        RateLimitInfo info = getRateLimitInfo(redisKey, isAuthenticated);
        
        // 현재 시간 기준으로 토큰 재계산
        LocalDateTime now = LocalDateTime.now();
        double currentTokens = calculateNewTokens(info, now);
        info.setTokens(currentTokens);
        info.setLastRefillTime(now);
        
        return info;
    }

    /**
     * 특정 사용자의 Rate Limit 초기화 (관리자용)
     */
    public void resetRateLimit(String userId, boolean isAuthenticated) {
        String redisKey = getRedisKey(userId, isAuthenticated);
        redisTemplate.delete(redisKey);
        log.info("Rate limit reset for user: {} (authenticated: {})", userId, isAuthenticated);
    }
}
