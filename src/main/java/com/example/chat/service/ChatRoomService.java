package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessagePublisher messagePublisher;

    public ChatRoomService(RedisTemplate<String, Object> redisTemplate, MessagePublisher messagePublisher) {
        this.redisTemplate = redisTemplate;
        this.messagePublisher = messagePublisher;
    }

    private static final String ROOM_MEMBERS_PREFIX = "room:members:";
    private static final String USER_ROOMS_PREFIX = "user:rooms:";
    
    public void joinRoom(String roomId, String userId) {
        // 채팅방에 사용자 추가
        redisTemplate.opsForSet().add(ROOM_MEMBERS_PREFIX + roomId, userId);
        
        // 사용자가 속한 채팅방 목록에 추가
        redisTemplate.opsForSet().add(USER_ROOMS_PREFIX + userId, roomId);
        
        log.info("User joined room - RoomId: {}, UserId: {}", roomId, userId);
    }
    
    public void leaveRoom(String roomId, String userId) {
        // 채팅방에서 사용자 제거
        redisTemplate.opsForSet().remove(ROOM_MEMBERS_PREFIX + roomId, userId);
        
        // 사용자의 채팅방 목록에서 제거
        redisTemplate.opsForSet().remove(USER_ROOMS_PREFIX + userId, roomId);
        
        log.info("User left room - RoomId: {}, UserId: {}", roomId, userId);
    }
    
    public Set<Object> getRoomMembers(String roomId) {
        return redisTemplate.opsForSet().members(ROOM_MEMBERS_PREFIX + roomId);
    }
    
    public Set<Object> getUserRooms(String userId) {
        return redisTemplate.opsForSet().members(USER_ROOMS_PREFIX + userId);
    }
    
    public boolean isUserInRoom(String roomId, String userId) {
        return redisTemplate.opsForSet().isMember(ROOM_MEMBERS_PREFIX + roomId, userId);
    }
    
    public void broadcastToRoom(String roomId, ChatMessage message) {
        Set<Object> members = getRoomMembers(roomId);
        
        for (Object member : members) {
            String memberId = member.toString();
            
            // 발신자 제외
            if (!memberId.equals(message.getSenderId())) {
                ChatMessage roomMessage = new ChatMessage();
                roomMessage.setId(message.getId());
                roomMessage.setSenderId(message.getSenderId());
                roomMessage.setReceiverId(memberId);
                roomMessage.setRoomId(roomId);
                roomMessage.setContent(message.getContent());
                roomMessage.setType(message.getType());
                roomMessage.setTimestamp(message.getTimestamp());
                
                messagePublisher.publishMessage(roomMessage);
            }
        }
        
        log.info("Message broadcast to room - RoomId: {}, MemberCount: {}", 
                roomId, members.size() - 1);
    }
}
