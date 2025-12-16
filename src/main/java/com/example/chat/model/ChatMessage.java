package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private MessageType type;
    private String roomId;
    private LocalDateTime timestamp;

    // 추가 필드들
    private String senderId;  // sender와 동일, 호환성을 위해 유지
    private String receiverId; // receiver와 동일, 호환성을 위해 유지
    private MessageStatus status;
    private String attachmentUrl;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        TYPING,
        READ
    }

    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ
    }

    // 간단한 메시지 생성을 위한 정적 팩토리 메서드
    public static ChatMessage createJoinMessage(String userId) {
        return ChatMessage.builder()
                .sender(userId)
                .senderId(userId)
                .content(userId + " joined")
                .type(MessageType.JOIN)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatMessage createLeaveMessage(String userId) {
        return ChatMessage.builder()
                .sender(userId)
                .senderId(userId)
                .content(userId + " left")
                .type(MessageType.LEAVE)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
