package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_client_identifier", columnList = "clientIdentifier", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @PrePersist
    public void prePersist() {
        if (this.userId == null) {
            this.userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}