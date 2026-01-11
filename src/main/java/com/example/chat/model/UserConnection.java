package com.example.chat.model;

import java.util.Objects;

public class UserConnection {
    private String userId;
    private String sessionId;
    private String serverId;
    private long connectedAt;

    public UserConnection() {}

    public UserConnection(String userId, String sessionId, String serverId, long connectedAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.serverId = serverId;
        this.connectedAt = connectedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getServerId() {
        return serverId;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setConnectedAt(long connectedAt) {
        this.connectedAt = connectedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConnection that = (UserConnection) o;
        return connectedAt == that.connectedAt &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(serverId, that.serverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sessionId, serverId, connectedAt);
    }

    @Override
    public String toString() {
        return "UserConnection{" +
                "userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", connectedAt=" + connectedAt +
                '}';
    }
}
