package com.example.chat.model;

import java.time.LocalDateTime;
import java.util.Objects;

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

    // 기본 생성자
    public ChatMessage() {
    }

    // 전체 필드 생성자
    public ChatMessage(String id, String sender, String receiver, String content, 
                      MessageType type, String roomId, LocalDateTime timestamp,
                      String senderId, String receiverId, MessageStatus status, 
                      String attachmentUrl) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.roomId = roomId;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.attachmentUrl = attachmentUrl;
    }

    // Getter 메서드들
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {
        return type;
    }

    public String getRoomId() {
        return roomId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    // Setter 메서드들
    public void setId(String id) {
        this.id = id;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    // Builder 패턴 구현
    public static class Builder {
        private String id;
        private String sender;
        private String receiver;
        private String content;
        private MessageType type;
        private String roomId;
        private LocalDateTime timestamp;
        private String senderId;
        private String receiverId;
        private MessageStatus status;
        private String attachmentUrl;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder receiver(String receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder roomId(String roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder receiverId(String receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder status(MessageStatus status) {
            this.status = status;
            return this;
        }

        public Builder attachmentUrl(String attachmentUrl) {
            this.attachmentUrl = attachmentUrl;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(id, sender, receiver, content, type, roomId, 
                                 timestamp, senderId, receiverId, status, attachmentUrl);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // 간단한 메시지 생성을 위한 정적 팩토리 메서드
    public static ChatMessage createJoinMessage(String userId) {
        ChatMessage message = new ChatMessage();
        message.sender = userId;
        message.senderId = userId;
        message.content = userId + " joined";
        message.type = MessageType.JOIN;
        message.timestamp = LocalDateTime.now();
        return message;
    }
    
    public static ChatMessage createLeaveMessage(String userId) {
        ChatMessage message = new ChatMessage();
        message.sender = userId;
        message.senderId = userId;
        message.content = userId + " left";
        message.type = MessageType.LEAVE;
        message.timestamp = LocalDateTime.now();
        return message;
    }

    // equals, hashCode, toString 메서드 구현
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(sender, that.sender) &&
               Objects.equals(receiver, that.receiver) &&
               Objects.equals(content, that.content) &&
               type == that.type &&
               Objects.equals(roomId, that.roomId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(senderId, that.senderId) &&
               Objects.equals(receiverId, that.receiverId) &&
               status == that.status &&
               Objects.equals(attachmentUrl, that.attachmentUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, receiver, content, type, roomId, 
                          timestamp, senderId, receiverId, status, attachmentUrl);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", roomId='" + roomId + '\'' +
                ", timestamp=" + timestamp +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", status=" + status +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                '}';
    }
}
