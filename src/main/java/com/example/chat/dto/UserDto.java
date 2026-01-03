package com.example.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private String userId;
    private String nickname;
    private LocalDateTime createdAt;
    private boolean isNewUser;
}