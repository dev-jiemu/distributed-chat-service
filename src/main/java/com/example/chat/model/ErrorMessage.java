package com.example.chat.model;

import java.time.LocalDateTime;

/**
 * 클라이언트에게 전송할 에러 메시지
 * WebSocket을 통해 /user/queue/errors로 전송됨
 */
public class ErrorMessage {
    
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private Long retryAfterSeconds;  // Rate Limit의 경우 재시도 가능 시간

    public ErrorMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorMessage(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorMessage(String errorCode, String message, Long retryAfterSeconds) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", retryAfterSeconds=" + retryAfterSeconds +
                '}';
    }
}
