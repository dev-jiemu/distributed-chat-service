package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.concurrent.TimeUnit;

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

    public User findOrCreateUser(String ipAddress, String userAgent, String nickname, String userIdFromRequest) throws JsonProcessingException {
        // userIdFromRequest가 제공되면 먼저 userId로 찾아봅니다.
        if (userIdFromRequest != null && !userIdFromRequest.trim().isEmpty()) {
            Optional<User> userById = userRepository.findByUserId(userIdFromRequest);
            if (userById.isPresent()) {
                User user = userById.get();
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                cacheUser(user);
                log.info("Existing user logged in by userId: {}", user.getUserId());
                return user;
            } else {
                log.warn("User with userId {} not found, falling back to clientIdentifier based login.", userIdFromRequest);
            }
        }

        String clientIdentifier = generateClientIdentifier(ipAddress, userAgent);

        // 캐시 먼저 확인
        User cachedUser = getCachedUser(clientIdentifier);
        if (cachedUser != null) {
            log.debug("User found in cache: {}", cachedUser.getUserId());
            // 캐시된 유저의 lastLoginAt을 업데이트하고 다시 캐시 (비동기적으로 처리하거나, 다음 로그인 시 업데이트될 수 있도록 허용)
            // 여기서는 매번 업데이트하지 않고, DB에서 로드될 때만 업데이트하도록 유지
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

    public Optional<User> findUserByUserId(String userId) {
        // 캐시 먼저 확인 (clientIdentifier가 아닌 userId로 캐시를 관리하는 방법도 고려)
        // 현재 캐시는 clientIdentifier 기반이므로, userId로는 직접 캐시에서 찾지 않음.
        // 필요하다면 userId -> clientIdentifier 매핑 캐시를 도입하거나, userId 기반 캐시를 별도로 구성.
        // 여기서는 직접 조회
        Optional<User> user = userRepository.findByUserId(userId);
        user.ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
            cacheUser(u); // Redis에 clientIdentifier 기반으로 캐시
        });
        return user;
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
        Object cachedObject = redisTemplate.opsForValue().get(key);
        if (cachedObject instanceof User) {
            return (User) cachedObject;
        }
        return null;
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

    public User registerUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) { // 이미 있으면 null return
            return null;
        }

        User newUser = new User();
        newUser.setEmail(email);
        // 암호화 적용
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setUserType(com.example.chat.entity.UserType.AUTHENTICATED);
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
            cacheUser(user);
        });
    }
}
