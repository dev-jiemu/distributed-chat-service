package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final long USER_CACHE_TTL = 3600; // 1시간만 저장할거임

    public User findOrCreateUser(String ipAddress, String userAgent, String nickname) throws JsonProcessingException {
        String clientIdentifier = generateClientIdentifier(ipAddress, userAgent);

        // 캐시 먼저 확인
        User cachedUser = getCachedUser(clientIdentifier);
        if (cachedUser != null) {
            log.debug("User found in cache: {}", cachedUser.getUserId());
            return cachedUser;
        }

        // 없으면 DB에서 조회
        Optional<User> existingUser = userRepository.findByClientIdentifier(clientIdentifier);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            // 캐시에 저장
            cacheUser(user);
            log.info("Existing user logged in: {}", user.getUserId());
            return user;
        }

        User newUser = createNewUser(ipAddress, userAgent, nickname, clientIdentifier);
        cacheUser(newUser);
        log.info("New user created: {}", newUser.getUserId());
        return newUser;
    }

    private User createNewUser(String ipAddress, String userAgent, String nickname, String clientIdentifier) throws JsonProcessingException {
        // 닉네임 중복 체크 및 자동 생성
        String finalNickname = generateUniqueNickname(nickname);

        User user = new User();
        user.setClientIdentifier(clientIdentifier);
        user.setIpAddress(ipAddress);
        user.setUserAgent(userAgent);
        user.setNickname(finalNickname);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());

        // 디바이스 정보 추가
        Map<String, String> deviceInfo = parseDeviceInfo(userAgent);
        user.setDeviceInfo(new ObjectMapper().writeValueAsString(deviceInfo));

        return userRepository.save(user);
    }

    private String generateClientIdentifier(String ipAddress, String userAgent) {
        String combined = ipAddress + ":" + userAgent;
        return DigestUtils.sha256Hex(combined);
    }

    private User getCachedUser(String clientIdentifier) {
        String key = USER_CACHE_PREFIX + clientIdentifier;
        return (User) redisTemplate.opsForValue().get(key);
    }

    private void cacheUser(User user) {
        String key = USER_CACHE_PREFIX + user.getClientIdentifier();
        redisTemplate.opsForValue().set(key, user, USER_CACHE_TTL, TimeUnit.SECONDS);
    }

    // TODO : front 랑 맞춰보기
    private Map<String, String> parseDeviceInfo(String userAgent) {
        Map<String, String> info = new HashMap<>();

        if (userAgent.contains("Mobile")) {
            info.put("deviceType", "mobile");
        } else {
            info.put("deviceType", "desktop");
        }

        return info;
    }

    private String generateUniqueNickname(String requestedNickname) {
        if (requestedNickname == null || requestedNickname.trim().isEmpty()) {
            requestedNickname = "Guest";
        }

        String baseNickname = requestedNickname.trim();
        String finalNickname = baseNickname;
        int counter = 1;

        while (userRepository.existsByNickname(finalNickname)) {
            finalNickname = baseNickname + counter;
            counter++;
        }

        return finalNickname;
    }

}
