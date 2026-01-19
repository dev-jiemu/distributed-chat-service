package com.example.chat.controller;

import com.example.chat.dto.UserDto;
import com.example.chat.entity.User;
import com.example.chat.model.*;
import com.example.chat.service.JwtService;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Autowired
    public AuthController(
            UserService userService,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 익명 사용자 로그인 (기존 로직 유지)
     */
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

    /**
     * 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = new RegisterResponse();

        // 비밀번호 해시화 전에 UserService에 전달
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
        response.setNickname(newUser.getNickname());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 이메일/패스워드 인증 (JWT 토큰 발급)
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            // 1. 이메일로 사용자 조회
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "이메일 또는 패스워드가 올바르지 않습니다."));
            }
            
            // 2. Spring Security 인증 수행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUserId(),  // userId로 인증
                            request.getPassword()
                    )
            );
            
            // 3. 인증 성공 시 JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(user.getUserId());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId());
            
            // 4. 마지막 로그인 시간 업데이트
            userService.updateLastLoginTime(user.getUserId());
            
            // 5. 응답 생성
            LoginResponse response = new LoginResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    accessTokenExpiration / 1000  // 초 단위로 변환
            );
            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "이메일 또는 패스워드가 올바르지 않습니다."));
        } catch (Exception e) {
            log.error("인증 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "인증 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody TokenRefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // 1. 리프레시 토큰 검증
            if (!jwtService.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 리프레시 토큰입니다."));
            }
            
            // 2. 리프레시 토큰인지 확인
            if (!jwtService.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "리프레시 토큰이 아닙니다."));
            }
            
            // 3. 토큰에서 userId 추출
            String userId = jwtService.getUserIdFromToken(refreshToken);
            
            // 4. 사용자 존재 확인
            User user = userService.findByUserId(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }
            
            // 5. 새로운 토큰 발급
            String newAccessToken = jwtService.generateAccessToken(userId);
            String newRefreshToken = jwtService.generateRefreshToken(userId);
            
            TokenRefreshResponse response = new TokenRefreshResponse(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    accessTokenExpiration / 1000  // 초 단위로 변환
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "토큰 갱신 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃
     * 클라이언트에서 토큰을 삭제하는 방식으로 처리
     * 필요시 토큰 블랙리스트를 Redis에 저장할 수 있음
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String userId = authentication.getName();
                log.info("사용자 로그아웃: {}", userId);
                
                // TODO : 필요시 Redis에 토큰 블랙리스트 추가
                // 예: redisTemplate.opsForValue().set("blacklist:" + token, userId, accessTokenExpiration, TimeUnit.MILLISECONDS);
            }
            
            // SecurityContext 클리어
            SecurityContextHolder.clearContext();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "로그아웃되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "로그아웃 처리 중 오류가 발생했습니다."));
        }
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
