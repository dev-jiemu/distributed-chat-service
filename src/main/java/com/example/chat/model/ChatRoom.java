package com.example.chat.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChatRoom {
    private String roomId;
    private String roomName;
    private Set<String> participants = new HashSet<>();
    private LocalDateTime createdAt;
    private String createdBy;

    public ChatRoom() {}

    public ChatRoom(String roomId, String roomName, Set<String> participants, LocalDateTime createdAt, String createdBy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.participants = participants;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<String> participants) {
        this.participants = participants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return Objects.equals(roomId, chatRoom.roomId) && Objects.equals(roomName, chatRoom.roomName) && Objects.equals(participants, chatRoom.participants) && Objects.equals(createdAt, chatRoom.createdAt) && Objects.equals(createdBy, chatRoom.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, roomName, participants, createdAt, createdBy);
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", participants=" + participants +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }

    public void addParticipant(String userId) {
        this.participants.add(userId);
    }
    
    public void removeParticipant(String userId) {
        this.participants.remove(userId);
    }
    
    public boolean hasParticipant(String userId) {
        return this.participants.contains(userId);
    }
}
