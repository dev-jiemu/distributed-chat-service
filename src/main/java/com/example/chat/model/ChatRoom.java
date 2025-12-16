package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private String roomId;
    private String roomName;
    private Set<String> participants = new HashSet<>();
    private LocalDateTime createdAt;
    private String createdBy;
    
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
