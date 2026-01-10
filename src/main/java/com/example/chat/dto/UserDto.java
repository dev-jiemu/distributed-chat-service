package com.example.chat.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserDto {
    private String userId;
    private String nickname;
    private LocalDateTime createdAt;
    private boolean isNewUser;

    public UserDto(String userId, String nickname, LocalDateTime createdAt, boolean isNewUser) {
        this.userId = userId;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.isNewUser = isNewUser;
    }

    public static UserDtoBuilder builder() {
        return new UserDtoBuilder();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean newUser) {
        isNewUser = newUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDto userDto = (UserDto) o;
        return isNewUser == userDto.isNewUser && Objects.equals(userId, userDto.userId) && Objects.equals(nickname, userDto.nickname) && Objects.equals(createdAt, userDto.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, nickname, createdAt, isNewUser);
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", createdAt=" + createdAt +
                ", isNewUser=" + isNewUser +
                '}';
    }

    public static class UserDtoBuilder {
        private String userId;
        private String nickname;
        private LocalDateTime createdAt;
        private boolean isNewUser;

        UserDtoBuilder() {
        }

        public UserDtoBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserDtoBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDtoBuilder isNewUser(boolean isNewUser) {
            this.isNewUser = isNewUser;
            return this;
        }

        public UserDto build() {
            return new UserDto(userId, nickname, createdAt, isNewUser);
        }

        public String toString() {
            return "UserDto.UserDtoBuilder(userId=" + this.userId + ", nickname=" + this.nickname + ", createdAt=" + this.createdAt + ", isNewUser=" + this.isNewUser + ")";
        }
    }
}