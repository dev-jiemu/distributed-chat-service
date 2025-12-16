package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConnection {
    private String userId;
    private String sessionId;
    private String serverId;
    private long connectedAt;
}
