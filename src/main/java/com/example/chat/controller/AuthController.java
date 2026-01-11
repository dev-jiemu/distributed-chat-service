package com.example.chat.controller;

import com.example.chat.dto.UserDto;
import com.example.chat.entity.User;
import com.example.chat.model.LoginRequest;
import com.example.chat.model.RegisterRequest;
import com.example.chat.model.RegisterResponse;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) throws JsonProcessingException {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userService.findOrCreateUser(ipAddress, userAgent, request.getNickname(), request.getUserId());

        UserDto userDto = UserDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .isNewUser(user.getCreatedAt().equals(user.getLastLoginAt()))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = new RegisterResponse(null, null, null, null, false, null); // Initialize with default values

        User newUser = userService.registerUser(request.getEmail(), request.getPassword());

        if (newUser == null) {
            response.setSuccess(false);
            response.setMessage("이미 사용 중인 이메일 주소입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        response.setSuccess(true);
        response.setMessage("회원가입이 성공적으로 완료되었습니다.");
        response.setUserId(newUser.getUserId());
        response.setEmail(newUser.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // TODO : add
    @PostMapping("/authenticate")
    public ResponseEntity<UserDto> authenticate() {
        return ResponseEntity.ok(null);
    }

    // TODO : add
    @PostMapping("/refresh")
    public ResponseEntity<UserDto> refresh() {
        return ResponseEntity.ok(null);
    }

    // TODO : add
    @PostMapping("/logout")
    public ResponseEntity<UserDto> logout() {
        return ResponseEntity.ok(null);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

