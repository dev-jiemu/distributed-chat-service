package com.example.chat.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_user_id", columnList = "userId")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;  // user_xxxxx 형태

    @Column(nullable = false)
    private String nickname;

    @Column(unique = true)
    private String email;

    @Column
    private String passwordHash;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastActiveAt;

    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime emailVerifiedAt;

    @Column
    private boolean isAccountUpgraded = false;
    
    // 추가 정보 (선택사항)
    @Column(columnDefinition = "TEXT")
    private String deviceInfo;  // JSON 형태로 디바이스 정보 저장

    public User() {}

    public User(Long id, String userId, String nickname, String email, LocalDateTime createdAt, LocalDateTime lastActiveAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public boolean isAccountUpgraded() {
        return isAccountUpgraded;
    }

    public void setAccountUpgraded(boolean accountUpgraded) {
        isAccountUpgraded = accountUpgraded;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return isAccountUpgraded == user.isAccountUpgraded && 
               Objects.equals(id, user.id) && 
               Objects.equals(userId, user.userId) && 
               Objects.equals(nickname, user.nickname) && 
               Objects.equals(email, user.email) && 
               Objects.equals(passwordHash, user.passwordHash) && 
               Objects.equals(createdAt, user.createdAt) && 
               Objects.equals(lastActiveAt, user.lastActiveAt) && 
               Objects.equals(lastLoginAt, user.lastLoginAt) && 
               Objects.equals(emailVerifiedAt, user.emailVerifiedAt) && 
               Objects.equals(deviceInfo, user.deviceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, nickname, email, passwordHash, createdAt, lastActiveAt, lastLoginAt, emailVerifiedAt, isAccountUpgraded, deviceInfo);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                ", lastLoginAt=" + lastLoginAt +
                ", emailVerifiedAt=" + emailVerifiedAt +
                ", isAccountUpgraded=" + isAccountUpgraded +
                '}';
    }
}