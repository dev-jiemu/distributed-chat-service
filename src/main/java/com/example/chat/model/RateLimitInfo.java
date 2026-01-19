package com.example.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Rate Limit 정보를 담는 클래스
 * Redis에 저장되어 Token Bucket 알고리즘에 사용됨
 */
public class RateLimitInfo {
    
    private double tokens;              // 현재 남은 토큰 수
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRefillTime;  // 마지막으로 토큰을 충전한 시간
    
    private int maxTokens;              // 토큰 바구니의 최대 용량 (버스트)
    private double refillRate;          // 초당 충전되는 토큰 비율

    public RateLimitInfo() {
    }

    public RateLimitInfo(double tokens, LocalDateTime lastRefillTime, int maxTokens, double refillRate) {
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
    }

    /**
     * 초기 상태 생성 (토큰 바구니가 가득 찬 상태)
     */
    public static RateLimitInfo initialize(int maxTokens, double refillRate) {
        return new RateLimitInfo(
            maxTokens,                  // 처음엔 토큰이 가득 참
            LocalDateTime.now(),
            maxTokens,
            refillRate
        );
    }

    // Getters and Setters
    public double getTokens() {
        return tokens;
    }

    public void setTokens(double tokens) {
        this.tokens = tokens;
    }

    public LocalDateTime getLastRefillTime() {
        return lastRefillTime;
    }

    public void setLastRefillTime(LocalDateTime lastRefillTime) {
        this.lastRefillTime = lastRefillTime;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(double refillRate) {
        this.refillRate = refillRate;
    }

    @Override
    public String toString() {
        return "RateLimitInfo{" +
                "tokens=" + tokens +
                ", lastRefillTime=" + lastRefillTime +
                ", maxTokens=" + maxTokens +
                ", refillRate=" + refillRate +
                '}';
    }
}
