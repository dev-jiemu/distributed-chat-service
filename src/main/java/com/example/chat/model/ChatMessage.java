package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    
    private String id;
    private String senderId;
    private String receiverId;
    private String roomId;  // 그룹 채팅용
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        TYPING,
        READ
    }
    
    public ChatMessage(String senderId, String content, MessageType type) {
        this.senderId = senderId;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
}
