package com.example.chat;

import com.example.chat.model.UserConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

@SpringBootTest
public class RedisDebugTest {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    public void testRedisData() {
        // 테스트 데이터 저장
        UserConnection testConnection = new UserConnection("testUser", "session123", "server1", System.currentTimeMillis());
        redisTemplate.opsForValue().set("connection:testUser", testConnection);
        
        // 데이터 읽기
        Object rawValue = redisTemplate.opsForValue().get("connection:testUser");
        System.out.println("Raw value type: " + (rawValue != null ? rawValue.getClass().getName() : "null"));
        System.out.println("Raw value: " + rawValue);
        
        if (rawValue instanceof Map) {
            System.out.println("Value is a Map!");
            Map<?, ?> map = (Map<?, ?>) rawValue;
            map.forEach((k, v) -> System.out.println(k + " = " + v));
        }
        
        // ObjectMapper로 변환 시도
        try {
            UserConnection converted = objectMapper.convertValue(rawValue, UserConnection.class);
            System.out.println("Converted successfully: " + converted);
        } catch (Exception e) {
            System.out.println("Conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
