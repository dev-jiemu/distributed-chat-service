package com.example.chat;

import java.io.Serializable;
import java.util.Objects;

public class ChatMessage implements Serializable {
    private String roomId;
    private String senderId;
    private String content;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String roomId, String senderId, String content, long timestamp) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return timestamp == that.timestamp && Objects.equals(roomId, that.roomId) && Objects.equals(senderId, that.senderId) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, senderId, content, timestamp);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "roomId='" + roomId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public static ChatMessageBuilder builder() {
        return new ChatMessageBuilder();
    }

    public static class ChatMessageBuilder {
        private String roomId;
        private String senderId;
        private String content;
        private long timestamp;

        ChatMessageBuilder() {
        }

        public ChatMessageBuilder roomId(String roomId) {
            this.roomId = roomId;
            return this;
        }

        public ChatMessageBuilder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public ChatMessageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public ChatMessageBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(roomId, senderId, content, timestamp);
        }

        @Override
        public String toString() {
            return "ChatMessage.ChatMessageBuilder(roomId=" + this.roomId + ", senderId=" + this.senderId + ", content=" + this.content + ", timestamp=" + this.timestamp + ")";
        }
    }
}
