package com.example.chat.model;

import java.util.Objects;

public class LoginRequest {
    private String nickname;
    private String userId;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequest that = (LoginRequest) o;
        return Objects.equals(nickname, that.nickname) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, userId);
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "nickname='" + nickname + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}