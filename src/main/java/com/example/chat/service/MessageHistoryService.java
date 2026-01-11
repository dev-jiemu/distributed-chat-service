package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageHistoryService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageHistoryService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public MessageHistoryService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private static final String MESSAGE_HISTORY_PREFIX = "messages:";
    private static final String ROOM_MESSAGE_PREFIX = "room:messages:";
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * 1:1 메시지 저장
     */
    public void saveDirectMessage(ChatMessage message) {
        String conversationKey = getConversationKey(message.getSender(), message.getReceiver());
        saveMessageToHistory(conversationKey, message);
    }
    
    /**
     * 채팅방 메시지 저장
     */
    public void saveRoomMessage(ChatMessage message) {
        String roomKey = ROOM_MESSAGE_PREFIX + message.getRoomId();
        saveMessageToHistory(roomKey, message);
    }
    
    /**
     * 1:1 대화 히스토리 조회
     */
    public List<ChatMessage> getDirectMessageHistory(String user1, String user2, int limit) {
        String conversationKey = getConversationKey(user1, user2);
        return getMessageHistory(conversationKey, limit);
    }
    
    /**
     * 채팅방 메시지 히스토리 조회
     */
    public List<ChatMessage> getRoomMessageHistory(String roomId, int limit) {
        String roomKey = ROOM_MESSAGE_PREFIX + roomId;
        return getMessageHistory(roomKey, limit);
    }
    
    private void saveMessageToHistory(String key, ChatMessage message) {
        try {
            // 메시지를 JSON으로 변환
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Redis sorted set에 저장 (timestamp를 score로 사용)
            redisTemplate.opsForZSet().add(key, messageJson, 
                message.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC));
            
            // 최대 크기 유지
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null && size > MAX_HISTORY_SIZE) {
                // 오래된 메시지 제거
                redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY_SIZE - 1);
            }
            
            log.debug("Message saved to history - Key: {}", key);
        } catch (Exception e) {
            log.error("Failed to save message to history", e);
        }
    }
    
    private List<ChatMessage> getMessageHistory(String key, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        
        try {
            // 최근 메시지부터 조회 (역순)
            Set<Object> messageSet = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);
            
            if (messageSet != null) {
                messages = messageSet.stream()
                    .map(obj -> {
                        try {
                            return objectMapper.readValue(obj.toString(), ChatMessage.class);
                        } catch (Exception e) {
                            log.error("Failed to parse message", e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to get message history", e);
        }
        
        return messages;
    }
    
    /**
     * 두 사용자 간의 대화 키 생성 (순서 무관)
     */
    private String getConversationKey(String user1, String user2) {
        // 알파벳 순으로 정렬하여 일관된 키 생성
        if (user1.compareTo(user2) < 0) {
            return MESSAGE_HISTORY_PREFIX + user1 + ":" + user2;
        } else {
            return MESSAGE_HISTORY_PREFIX + user2 + ":" + user1;
        }
    }
}
