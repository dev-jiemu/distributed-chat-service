package com.example.chat.model;

public class TokenRefreshRequest {
    private String refreshToken;

    public TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "TokenRefreshRequest{" +
                "refreshToken='" + refreshToken + '\'' +
                '}';
    }
}
