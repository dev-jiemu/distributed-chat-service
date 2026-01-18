package com.example.chat.exception;

/**
 * Rate Limit 초과 시 발생하는 예외
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final String userId;
    private final long retryAfterSeconds;  // 몇 초 후에 재시도 가능한지
    
    public RateLimitExceededException(String userId, long retryAfterSeconds) {
        super(String.format("Rate limit exceeded for user: %s. Retry after %d seconds", 
            userId, retryAfterSeconds));
        this.userId = userId;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String userId, long retryAfterSeconds, String message) {
        super(message);
        this.userId = userId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getUserId() {
        return userId;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
