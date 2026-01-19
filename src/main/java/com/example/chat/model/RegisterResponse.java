package com.example.chat.model;

import java.util.Objects;

public class RegisterResponse {
    private String userId;
    private String email;
    private String nickname;
    private String accessToken;
    private String refreshToken;
    private boolean success;
    private String message;

    public RegisterResponse() {}

    public RegisterResponse(String userId, String email, String nickname, String accessToken, String refreshToken, boolean success, String message) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.success = success;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RegisterResponse that = (RegisterResponse) o;
        return success == that.success && Objects.equals(userId, that.userId) && Objects.equals(email, that.email) && Objects.equals(accessToken, that.accessToken) && Objects.equals(refreshToken, that.refreshToken) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, accessToken, refreshToken, success, message);
    }

    @Override
    public String toString() {
        return "RegisterResponse{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
