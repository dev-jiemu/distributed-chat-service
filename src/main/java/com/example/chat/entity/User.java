package com.example.chat.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_client_identifier", columnList = "clientIdentifier", unique = true)
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;  // user_xxxxx 형태

    @Column(nullable = false, unique = true)
    private String clientIdentifier;  // IP + UserAgent 해시

    @Column(nullable = false)
    private String nickname;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String deviceInfo;  // JSON 형태로 추가 정보 저장

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastActiveAt;

    private LocalDateTime lastLoginAt;

    public User() {
    }

    public User(Long id, String userId, String clientIdentifier, String nickname, String ipAddress, String userAgent, String deviceInfo, LocalDateTime createdAt, LocalDateTime lastActiveAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.userId = userId;
        this.clientIdentifier = clientIdentifier;
        this.nickname = nickname;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceInfo = deviceInfo;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.lastLoginAt = lastLoginAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.userId == null) {
            this.userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(userId, user.userId) && Objects.equals(clientIdentifier, user.clientIdentifier) && Objects.equals(nickname, user.nickname) && Objects.equals(ipAddress, user.ipAddress) && Objects.equals(userAgent, user.userAgent) && Objects.equals(deviceInfo, user.deviceInfo) && Objects.equals(createdAt, user.createdAt) && Objects.equals(lastActiveAt, user.lastActiveAt) && Objects.equals(lastLoginAt, user.lastLoginAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, clientIdentifier, nickname, ipAddress, userAgent, deviceInfo, createdAt, lastActiveAt, lastLoginAt);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", clientIdentifier='" + clientIdentifier + '\'' +
                ", nickname='" + nickname + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}