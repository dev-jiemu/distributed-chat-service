package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RedisTemplate<String, Object> redisTemplate, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String USER_CACHE_PREFIX = "user:";
    private static final long USER_CACHE_TTL = 3600; // 1시간만 저장할거임

    public Optional<User> findUserByUserId(String userId) {
        // 여기서는 직접 조회
        Optional<User> user = userRepository.findByUserId(userId);
        user.ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
        });
        return user;
    }

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

    public User registerUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) { // 이미 있으면 null return
            return null;
        }

        User newUser = new User();
        newUser.setEmail(email);
        // 암호화 적용
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setNickname(generateUniqueNickname(email.split("@")[0]));
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLoginAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

    public void updateLastLoginTime(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}
